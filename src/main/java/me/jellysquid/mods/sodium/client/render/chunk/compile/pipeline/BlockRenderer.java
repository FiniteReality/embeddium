package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.compat.forge.ForgeBlockRenderer;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
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
import net.minecraft.util.math.random.LocalRandom;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.common.ForgeConfig;
import org.embeddedt.embeddium.api.BlockRendererRegistry;

import java.util.Arrays;
import java.util.List;

public class BlockRenderer {
    private static final MatrixStack EMPTY_STACK = new MatrixStack();
    private final Random random = new LocalRandom(42L);

    private final ColorProviderRegistry colorProviderRegistry;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData quadLightData = new QuadLightData();

    private final LightPipelineProvider lighters;

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private final boolean useAmbientOcclusion;
    private final boolean useForgeExperimentalLightingPipeline;

    private final ForgeBlockRenderer forgeBlockRenderer = new ForgeBlockRenderer();

    private final int[] quadColors = new int[4];

    private boolean useReorienting;

    private final List<BlockRendererRegistry.Renderer> customRenderers = new ObjectArrayList<>();

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = MinecraftClient.isAmbientOcclusionEnabled();
        this.useForgeExperimentalLightingPipeline = ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
    }

    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forRenderLayer(ctx.renderLayer());
        var meshBuilder = buffers.get(material);

        ColorProvider<BlockState> colorizer = this.colorProviderRegistry.getColorProvider(ctx.state().getBlock());

        LightMode mode = this.getLightingMode(ctx.state(), ctx.model(), ctx.localSlice(), ctx.pos(), ctx.renderLayer());
        LightPipeline lighter = this.lighters.getLighter(mode);
        Vec3d renderOffset;
        
        if (ctx.state().hasModelOffset()) {
            renderOffset = ctx.state().getModelOffset(ctx.localSlice(), ctx.pos());
        } else {
            renderOffset = Vec3d.ZERO;
        }

        // Process custom renderers
        customRenderers.clear();
        BlockRendererRegistry.instance().fillCustomRenderers(customRenderers, ctx);

        if(!customRenderers.isEmpty()) {
            final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();
            for (BlockRendererRegistry.Renderer customRenderer : customRenderers) {
                builder.reset();
                BlockRendererRegistry.RenderResult result = customRenderer.renderBlock(ctx, random, builder);
                builder.flush(meshBuilder, material, ctx.origin());
                if (result == BlockRendererRegistry.RenderResult.OVERRIDE) {
                    return;
                }
            }
        }

        if(this.useForgeExperimentalLightingPipeline) {
            final MatrixStack mStack;
            if(renderOffset != Vec3d.ZERO) {
                mStack = new MatrixStack();
                mStack.translate(renderOffset.x, renderOffset.y, renderOffset.z);
            } else
                mStack = EMPTY_STACK;

            final SinkingVertexBuilder builder = SinkingVertexBuilder.getInstance();
            builder.reset();
            forgeBlockRenderer.renderBlock(mode, ctx, builder, mStack, this.random, this.occlusionCache, meshBuilder);
            builder.flush(meshBuilder, material, ctx.origin());
            return;
        }

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = this.getGeometry(ctx, face);

            if (!quads.isEmpty() && this.isFaceVisible(ctx, face)) {
                this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, meshBuilder, quads, face);
            }
        }

        List<BakedQuad> all = this.getGeometry(ctx, null);

        if (!all.isEmpty()) {
            this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, meshBuilder, all, null);
        }
    }

    private List<BakedQuad> getGeometry(BlockRenderContext ctx, Direction face) {
        var random = this.random;
        random.setSeed(ctx.seed());

        return ctx.model().getQuads(ctx.state(), face, random, ctx.modelData(), ctx.renderLayer());
    }

    private boolean isFaceVisible(BlockRenderContext ctx, Direction face) {
        return this.occlusionCache.shouldDrawSide(ctx.state(), ctx.localSlice(), ctx.pos(), face);
    }

    private void renderQuadList(BlockRenderContext ctx, Material material, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3d offset,
                                ChunkModelBuilder builder, List<BakedQuad> quads, Direction cullFace) {

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
            BakedQuadView quad = (BakedQuadView) quads.get(i);

            final var lightData = this.getVertexLight(ctx, quad.hasAmbientOcclusion() ? lighter : this.lighters.getLighter(LightMode.FLAT), cullFace, quad);
            final var vertexColors = this.getVertexColors(ctx, colorizer, quad);

            this.writeGeometry(ctx, builder, offset, material, quad, vertexColors, lightData);

            Sprite sprite = quad.getSprite();

            if (sprite != null) {
                builder.addSprite(sprite);
            }
        }
    }

    private QuadLightData getVertexLight(BlockRenderContext ctx, LightPipeline lighter, Direction cullFace, BakedQuadView quad) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, ctx.pos(), light, cullFace, quad.getLightFace(), quad.hasShade());

        return light;
    }

    private int[] getVertexColors(BlockRenderContext ctx, ColorProvider<BlockState> colorProvider, BakedQuadView quad) {
        final int[] vertexColors = this.quadColors;

        if (colorProvider != null && quad.hasColor()) {
            colorProvider.getColors(ctx.world(), ctx.pos(), ctx.state(), quad, vertexColors);
        } else {
            Arrays.fill(vertexColors, 0xFFFFFFFF);
        }

        return vertexColors;
    }

    private void writeGeometry(BlockRenderContext ctx,
                               ChunkModelBuilder builder,
                               Vec3d offset,
                               Material material,
                               BakedQuadView quad,
                               int[] colors,
                               QuadLightData light)
    {
        ModelQuadOrientation orientation = this.useReorienting ? ModelQuadOrientation.orientByBrightness(light.br, light.lm) : ModelQuadOrientation.NORMAL;
        var vertices = this.vertices;

        ModelQuadFacing normalFace = quad.getNormalFace();

        for (int dstIndex = 0; dstIndex < 4; dstIndex++) {
            int srcIndex = orientation.getVertexIndex(dstIndex);

            var out = vertices[dstIndex];
            out.x = ctx.origin().x() + quad.getX(srcIndex) + (float) offset.getX();
            out.y = ctx.origin().y() + quad.getY(srcIndex) + (float) offset.getY();
            out.z = ctx.origin().z() + quad.getZ(srcIndex) + (float) offset.getZ();

            out.color = ColorABGR.withAlpha(ModelQuadUtil.mixARGBColors(colors[srcIndex], quad.getColor(srcIndex)), light.br[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), light.lm[srcIndex]);
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);
    }

    private LightMode getLightingMode(BlockState state, BakedModel model, BlockRenderView world, BlockPos pos, RenderLayer renderLayer) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion(state, renderLayer) && state.getLightEmission(world, pos) == 0) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
