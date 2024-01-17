package me.jellysquid.mods.sodium.client.render.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
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
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.client.world.biome.BlockColorsExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import org.embeddedt.embeddium.api.BlockRendererRegistry;

import java.util.List;

public class BlockRenderer {
    private static final int[] DEFAULT_QUAD_COLORS = new int[] { -1, -1, -1, -1 };
    private static final PoseStack EMPTY_STACK = new PoseStack();

    private final BlockColorsExtended blockColors;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData cachedQuadLightData = new QuadLightData();

    private final ColorBlender colorBlender;
    private final ForgeBlockRenderer forgeBlockRenderer = new ForgeBlockRenderer();

    private final LightPipelineProvider lighters;

    private final boolean useAmbientOcclusion;

    private boolean useReorienting;
    private final List<BlockRendererRegistry.Renderer> customRenderers = new ObjectArrayList<>();

    public BlockRenderer(Minecraft client, LightPipelineProvider lighters, ColorBlender colorBlender) {
        this.blockColors = (BlockColorsExtended) client.getBlockColors();
        this.colorBlender = colorBlender;

        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
    }

    public boolean renderModel(BlockAndTintGetter world, BlockState state, BlockPos pos, BlockPos origin, BakedModel model, ChunkModelBuilder buffers, boolean cull, long seed, ModelData modelData, RenderType layer, RandomSource random) {
        LightMode mode = this.getLightingMode(state, model, world, pos, layer);
        LightPipeline lighter = this.lighters.getLighter(mode);
        Vec3 offset = state.getOffset(world, pos);

        boolean rendered = false;

        // Process custom renderers
        customRenderers.clear();
        BlockRendererRegistry.instance().fillCustomRenderers(customRenderers, state, pos, world, layer);

        if(!customRenderers.isEmpty()) {
            final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();
            for (BlockRendererRegistry.Renderer customRenderer : customRenderers) {
                builder.reset();
                BlockRendererRegistry.RenderResult result = customRenderer.renderBlock(state, pos, world, builder, random, modelData, layer);
                builder.flush(buffers, origin);
                if (result == BlockRendererRegistry.RenderResult.OVERRIDE) {
                    return true;
                }
            }
        }

        // Delegate to Forge render pipeline if enabled
        if(ForgeBlockRenderer.useForgeLightingPipeline()) {
            PoseStack mStack;
            if(offset != Vec3.ZERO) {
                mStack = new PoseStack();
                mStack.pushPose();
                mStack.translate(offset.x, offset.y, offset.z);
            } else
                mStack = EMPTY_STACK;
            final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();
            builder.reset();
            rendered = forgeBlockRenderer.renderBlock(mode, state, pos, world, model, mStack, builder, random, seed, modelData, cull, this.occlusionCache, buffers, layer);
            builder.flush(buffers, origin);
            return rendered;
        }

        // Use Sodium's default render path

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

    private void renderQuadList(BlockAndTintGetter world, BlockState state, BlockPos pos, BlockPos origin, LightPipeline lighter, Vec3 offset,
                                ChunkModelBuilder buffers, List<BakedQuad> quads, Direction cullFace) {
    	ModelQuadFacing facing = cullFace == null ? ModelQuadFacing.UNASSIGNED : ModelQuadFacing.fromDirection(cullFace);
        ColorSampler<BlockState> colorizer = null;

        ModelVertexSink vertices = buffers.getVertexSink();
        vertices.ensureCapacity(quads.size() * 4);

        IndexBufferBuilder indices = buffers.getIndexBufferBuilder(facing);

        this.useReorienting = true;

        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            if (!quads.get(i).hasAmbientOcclusion()) {
                // We disable Sodium's quad orientation detection if a quad opts out of AO. This is
                // because some mods place non-AO quads below/above AO quads with identical coordinates.
                // This won't z-fight as-is, but if the AO quad gets reoriented it can be triangulated
                // differently from the non-AO quad, and that will cause z-fighting.
                this.useReorienting = false;
                break;
            }
        }

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuad quad = quads.get(i);

            LightPipeline quadLighter = !quad.hasAmbientOcclusion() ? this.lighters.getLighter(LightMode.FLAT) : lighter;
            QuadLightData light = this.cachedQuadLightData;
            quadLighter.calculate((ModelQuadView) quad, pos, light, cullFace, quad.getDirection(), quad.isShade());

            if (quad.isTinted() && colorizer == null) {
                colorizer = this.blockColors.getColorProvider(state);
            }

            this.renderQuad(world, state, pos, origin, vertices, indices, offset, colorizer, quad, light, buffers);
        }

        vertices.flush();
    }

    private void renderQuad(BlockAndTintGetter world, BlockState state, BlockPos pos, BlockPos origin, ModelVertexSink vertices, IndexBufferBuilder indices, Vec3 blockOffset,
                            ColorSampler<BlockState> colorSampler, BakedQuad bakedQuad, QuadLightData light, ChunkModelBuilder model) {
        ModelQuadView src = (ModelQuadView) bakedQuad;
        ModelQuadOrientation orientation = this.useReorienting ? ModelQuadOrientation.orientByBrightness(light.br) : ModelQuadOrientation.NORMAL;

        int[] colors;

        if (bakedQuad.isTinted()) {
            colors = this.colorBlender.getColors(world, pos, src, colorSampler, state);
        } else {
            colors = DEFAULT_QUAD_COLORS;
        }

        int vertexStart = vertices.getVertexCount();

        for (int i = 0; i < 4; i++) {
            int j = orientation.getVertexIndex(i);

            float x = src.getX(j) + (float) blockOffset.x();
            float y = src.getY(j) + (float) blockOffset.y();
            float z = src.getZ(j) + (float) blockOffset.z();

            int color = ColorABGR.mul(ModelQuadUtil.mixARGBColors(colors[j], src.getColor(j)), light.br[j]);

            float u = src.getTexU(j);
            float v = src.getTexV(j);

            int lm = ModelQuadUtil.mergeBakedLight(src.getLight(j), light.lm[j]);

            vertices.writeVertex(origin, x, y, z, color, u, v, lm, model.getChunkId());
        }

        indices.add(vertexStart, ModelQuadWinding.CLOCKWISE);

        TextureAtlasSprite sprite = src.getSprite();

        if (sprite != null) {
            model.addSprite(sprite);
        }
    }

    @Deprecated
    @SuppressWarnings("unused")
    private LightMode getLightingMode(BlockState state, BakedModel model, BlockAndTintGetter world, BlockPos pos) {
        return getLightingMode(state, model, world, pos, RenderType.solid());
    }

    private LightMode getLightingMode(BlockState state, BakedModel model, BlockAndTintGetter world, BlockPos pos, RenderType layer) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion(state, layer) && state.getLightEmission(world, pos) == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
