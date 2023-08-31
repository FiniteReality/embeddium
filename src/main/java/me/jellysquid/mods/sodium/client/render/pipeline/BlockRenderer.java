package me.jellysquid.mods.sodium.client.render.pipeline;

import codechicken.lib.render.block.ICCBlockRenderer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.compat.ccl.CCLCompat;
import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.compat.forge.ForgeBlockRenderer;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;

public class BlockRenderer {

    private final BlockColorsExtended blockColors;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData cachedQuadLightData = new QuadLightData();

    private final ColorBlender colorBlender;
    private final ForgeBlockRenderer forgeBlockRenderer = new ForgeBlockRenderer();

    private final LightPipelineProvider lighters;

    private final boolean useAmbientOcclusion;

    public BlockRenderer(MinecraftClient client, LightPipelineProvider lighters, ColorBlender colorBlender) {
        this.blockColors = (BlockColorsExtended) client.getBlockColors();
        this.colorBlender = colorBlender;

        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
    }

    public boolean renderModel(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkModelBuilder buffers, boolean cull, long seed, ModelData modelData, RenderLayer layer, Random random) {
        LightMode mode = this.getLightingMode(state, model, world, pos);
        LightPipeline lighter = this.lighters.getLighter(mode);
        Vec3d offset = state.getModelOffset(world, pos);

        modelData = model.getModelData(world, pos, state, modelData);

        boolean rendered = false;

        if(SodiumClientMod.cclLoaded) {
            final MatrixStack mStack = new MatrixStack();
            final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();
            for (final ICCBlockRenderer renderer : CCLCompat.getCustomRenderers(world, pos)) {
                if (renderer.canHandleBlock(world, pos, state, layer)) {
                    mStack.isEmpty();

                    builder.reset();
                    renderer.renderBlock(state, pos, world, mStack, builder, random, modelData, layer);
                    rendered = true;
                    builder.flush(buffers, origin);

                    return rendered;
                }
            }
        }

        if(ForgeBlockRenderer.useForgeLightingPipeline()) {
            final MatrixStack mStack = new MatrixStack();
            if(offset != Vec3d.ZERO)
                mStack.translate(offset.x, offset.y, offset.z);

            final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();
            builder.reset();
            rendered = forgeBlockRenderer.renderBlock(mode, state, pos, world, model, mStack, builder, random, seed, modelData, cull, this.occlusionCache, buffers, layer);
            builder.flush(buffers, origin);
            return rendered;
        }

        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(seed);

            List<BakedQuad> sided = model.getQuads(state, dir, random, modelData, layer);

            if (sided.isEmpty()) {
                continue;
            }

            if (!cull || this.occlusionCache.shouldDrawSide(state, world, pos, dir)) {
            	this.renderQuadList(world, state, pos, origin, lighter, offset, buffers, sided, dir);

                rendered = true;
            }
        }

        random.setSeed(seed);

        List<BakedQuad> all = model.getQuads(state, null, random, modelData, layer);

        if (!all.isEmpty()) {
        	this.renderQuadList(world, state, pos, origin, lighter, offset, buffers, all, null);

            rendered = true;
        }

        return rendered;
    }

    private void renderQuadList(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, LightPipeline lighter, Vec3d offset,
                                ChunkModelBuilder buffers, List<BakedQuad> quads, Direction cullFace) {
    	ModelQuadFacing facing = cullFace == null ? ModelQuadFacing.UNASSIGNED : ModelQuadFacing.fromDirection(cullFace);
        ColorSampler<BlockState> colorizer = null;

        ModelVertexSink vertices = buffers.getVertexSink();
        vertices.ensureCapacity(quads.size() * 4);

        IndexBufferBuilder indices = buffers.getIndexBufferBuilder(facing);

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad quad = quads.get(i);

            QuadLightData light = this.cachedQuadLightData;
            lighter.calculate((ModelQuadView) quad, pos, light, cullFace, quad.getFace(), quad.hasShade());

            if (quad.hasColor() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            this.renderQuad(world, state, pos, origin, vertices, indices, offset, colorizer, quad, light, buffers);
        }

        vertices.flush();
    }

    private void renderQuad(BlockRenderView world, BlockState state, BlockPos pos, BlockPos origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3d blockOffset,
                            ColorSampler<BlockState> colorSampler, BakedQuad bakedQuad, QuadLightData light, ChunkModelBuilder model) {
        ModelQuadView src = (ModelQuadView) bakedQuad;
        ModelQuadOrientation orientation = ModelQuadOrientation.orientByBrightness(light.br);

        int[] colors = null;

        if (bakedQuad.hasColor()) {
            colors = this.colorBlender.getColors(world, pos, src, colorSampler, state);
        }

        int vertexStart = vertices.getVertexCount();

        for (int i = 0; i < 4; i++) {
            int j = orientation.getVertexIndex(i);

            float x = src.getX(j) + (float) blockOffset.getX();
            float y = src.getY(j) + (float) blockOffset.getY();
            float z = src.getZ(j) + (float) blockOffset.getZ();

            int color = ColorABGR.mul(colors != null ? colors[j] : src.getColor(j), light.br[j]);

            float u = src.getTexU(j);
            float v = src.getTexV(j);

            int lm = light.lm[j];

            vertices.writeVertex(origin, x, y, z, color, u, v, lm, model.getChunkId());
        }

        indices.add(vertexStart, ModelQuadWinding.CLOCKWISE);

        Sprite sprite = src.getSprite();

        if (sprite != null) {
            model.addSprite(sprite);
        }
    }

    private LightMode getLightingMode(BlockState state, BakedModel model, BlockRenderView world, BlockPos pos) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion() && state.getLightEmission(world, pos) == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
