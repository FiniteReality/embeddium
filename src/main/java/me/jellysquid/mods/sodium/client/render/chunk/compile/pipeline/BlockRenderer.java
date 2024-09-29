package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadOrientation;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
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
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.embeddedt.embeddium.api.model.EmbeddiumBakedModelExtension;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevel;
import org.embeddedt.embeddium.impl.render.chunk.sprite.SpriteTransparencyLevelHolder;
import org.embeddedt.embeddium.render.ShaderModBridge;
import org.embeddedt.embeddium.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.render.frapi.FRAPIModelUtils;
import org.embeddedt.embeddium.render.frapi.FRAPIRenderHandler;
import org.embeddedt.embeddium.render.frapi.IndigoBlockRenderContext;

import java.util.Arrays;
import java.util.List;

/**
 * The Embeddium equivalent to vanilla's ModelBlockRenderer. This is the primary component of the chunk meshing logic;
 * it is responsible for accepting {@link BlockRenderContext} and generating the appropriate geometry.
 * <p>
 * This class does not need to be thread-safe, as a separate instance is allocated per meshing thread.
 */
public class BlockRenderer {
    private static final PoseStack EMPTY_STACK = new PoseStack();
    private final RandomSource random = new SingleThreadedRandomSource(42L);

    private final ColorProviderRegistry colorProviderRegistry;
    private final BlockOcclusionCache occlusionCache;

    private final QuadLightData quadLightData = new QuadLightData();

    private final LightPipelineProvider lighters;

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();

    private final boolean useAmbientOcclusion;

    private final int[] quadColors = new int[4];

    /**
     * Tracks whether the MC-138211 quad reorienting fix should be applied during emission of quad geometry.
     * This fix must be disabled with certain modded models that use superimposed quads, as it can alter the triangulation
     * of some layers but not others, resulting in Z-fighting.
     */
    private boolean useReorienting;

    /**
     * The list of registered custom block renderers. These may augment or fully bypass the model system for the
     * block.
     */
    private final List<BlockRendererRegistry.Renderer> customRenderers = new ObjectArrayList<>();

    private final SinkingVertexBuilder sinkingVertexBuilder = new SinkingVertexBuilder();

    private final FRAPIRenderHandler fabricModelRenderingHandler;

    private final ChunkColorWriter colorEncoder = ChunkColorWriter.get();

    private final boolean useRenderPassOptimization;

    public BlockRenderer(ColorProviderRegistry colorRegistry, LightPipelineProvider lighters) {
        this.colorProviderRegistry = colorRegistry;
        this.lighters = lighters;

        this.occlusionCache = new BlockOcclusionCache();
        this.useAmbientOcclusion = Minecraft.useAmbientOcclusion();
        this.fabricModelRenderingHandler = FRAPIRenderHandler.INDIGO_PRESENT ? new IndigoBlockRenderContext(this.occlusionCache, lighters.getLightData()) : null;
        this.useRenderPassOptimization = SodiumClientMod.options().performance.useRenderPassOptimization && !ShaderModBridge.areShadersEnabled();
    }

    /**
     * Renders all geometry for a block into the given chunk build buffers.
     * @param ctx the context for the current block being rendered
     * @param buffers the buffer to output geometry to
     */
    public void renderModel(BlockRenderContext ctx, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forRenderLayer(ctx.renderLayer());
        var meshBuilder = buffers.get(material);

        ColorProvider<BlockState> colorizer = this.colorProviderRegistry.getColorProvider(ctx.state().getBlock());

        LightMode mode = this.getLightingMode(ctx.state(), ctx.model(), ctx.localSlice(), ctx.pos(), ctx.renderLayer());
        LightPipeline lighter = this.lighters.getLighter(mode);
        Vec3 renderOffset;
        
        if (ctx.state().hasOffsetFunction()) {
            renderOffset = ctx.state().getOffset(ctx.localSlice(), ctx.pos());
        } else {
            renderOffset = Vec3.ZERO;
        }

        // Process custom renderers
        customRenderers.clear();
        BlockRendererRegistry.instance().fillCustomRenderers(customRenderers, ctx);

        if(!customRenderers.isEmpty()) {
            for (BlockRendererRegistry.Renderer customRenderer : customRenderers) {
                sinkingVertexBuilder.reset();
                BlockRendererRegistry.RenderResult result = customRenderer.renderBlock(ctx, random, sinkingVertexBuilder);
                sinkingVertexBuilder.flush(meshBuilder, material, ctx.origin());
                if (result == BlockRendererRegistry.RenderResult.OVERRIDE) {
                    return;
                }
            }
        }

        // Delegate FRAPI models to their pipeline
        if (FRAPIModelUtils.isFRAPIModel(ctx.model())) {
            this.fabricModelRenderingHandler.reset();
            this.fabricModelRenderingHandler.renderEmbeddium(ctx, ctx.stack(), random);
            this.fabricModelRenderingHandler.flush(buffers, ctx.origin());
            return;
        }

        boolean canReorientNullCullface = true;

        for (Direction face : DirectionUtil.ALL_DIRECTIONS) {
            List<BakedQuad> quads = this.getGeometry(ctx, face);

            if (!quads.isEmpty() && this.isFaceVisible(ctx, face)) {
                this.useReorienting = true;
                this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, buffers, meshBuilder, quads, face);
                if (!this.useReorienting) {
                    // Reorienting was disabled on this side, make sure it's disabled for the null cullface too, in case
                    // a mod layers textures in different lists
                    canReorientNullCullface = false;
                }
            }
        }

        List<BakedQuad> all = this.getGeometry(ctx, null);

        if (!all.isEmpty()) {
            this.useReorienting = canReorientNullCullface;
            this.renderQuadList(ctx, material, lighter, colorizer, renderOffset, buffers, meshBuilder, all, null);
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

    private static int computeLightFlagMask(BakedQuad quad) {
        int flag = 0;

        if (quad.hasAmbientOcclusion()) {
            flag |= 1;
        }

        if (quad.isShade()) {
            flag |= 2;
        }

        return flag;
    }

    /**
     * {@return true if all quads in the given list use similar enough lighting configuration that reorientation is
     * unlikely to lead to z-fighting}
     */
    private static boolean checkQuadsHaveSameLightingConfig(List<BakedQuad> quads) {
        int quadsSize = quads.size();

        // By definition, singleton or empty lists of quads have a common lighting config. Only check larger lists
        if (quadsSize >= 2) {
            int flagMask = -1;
            // noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < quadsSize; i++) {
                int newFlag = computeLightFlagMask(quads.get(i));
                if (flagMask == -1) {
                    flagMask = newFlag;
                } else if(newFlag != flagMask) {
                    return false;
                }
            }
        }

        return true;
    }

    private ChunkModelBuilder chooseOptimalBuilder(Material defaultMaterial, ChunkBuildBuffers buffers, ChunkModelBuilder defaultBuilder, BakedQuadView quad) {
        if (defaultMaterial == DefaultMaterials.SOLID || !this.useRenderPassOptimization || (quad.getFlags() & ModelQuadFlags.IS_TRUSTED_SPRITE) == 0 || quad.getSprite() == null) {
            // No improvement possible
            return defaultBuilder;
        }

        SpriteTransparencyLevel level = SpriteTransparencyLevelHolder.getTransparencyLevel(quad.getSprite().contents());

        if (level == SpriteTransparencyLevel.OPAQUE && defaultMaterial.pass.supportsFragmentDiscard()) {
            // Can use solid with no visual difference
            return buffers.get(DefaultMaterials.SOLID);
        } else if (level == SpriteTransparencyLevel.TRANSPARENT && defaultMaterial == DefaultMaterials.TRANSLUCENT) {
            // Can use cutout_mipped with no visual difference
            return buffers.get(DefaultMaterials.CUTOUT_MIPPED);
        } else {
            // Have to use default
            return defaultBuilder;
        }
    }

    private void renderQuadList(BlockRenderContext ctx, Material material, LightPipeline lighter, ColorProvider<BlockState> colorizer, Vec3 offset,
                                ChunkBuildBuffers buffers, ChunkModelBuilder defaultBuilder, List<BakedQuad> quads, Direction cullFace) {

        if(!checkQuadsHaveSameLightingConfig(quads)) {
            // Disable reorienting if quads use different light configurations, as otherwise layered quads
            // may be triangulated differently from others in the stack, and that will cause z-fighting.
            this.useReorienting = false;
        }

        // This is a very hot allocation, iterate over it manually
        // noinspection ForLoopReplaceableByForEach
        for (int i = 0, quadsSize = quads.size(); i < quadsSize; i++) {
            BakedQuadView quad = (BakedQuadView) quads.get(i);

            final var lightData = this.getVertexLight(ctx, quad.hasAmbientOcclusion() ? lighter : this.lighters.getLighter(LightMode.FLAT), cullFace, quad);
            final var vertexColors = this.getVertexColors(ctx, colorizer, quad);

            ChunkModelBuilder builder = this.chooseOptimalBuilder(material, buffers, defaultBuilder, quad);

            this.writeGeometry(ctx, builder, offset, material, quad, vertexColors, lightData);

            TextureAtlasSprite sprite = quad.getSprite();

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
            // Force full alpha on all colors
            for(int i = 0; i < vertexColors.length; i++) {
                vertexColors[i] |= 0xFF000000;
            }
        } else {
            Arrays.fill(vertexColors, 0xFFFFFFFF);
        }

        return vertexColors;
    }

    private void writeGeometry(BlockRenderContext ctx,
                               ChunkModelBuilder builder,
                               Vec3 offset,
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
            out.x = ctx.origin().x() + quad.getX(srcIndex) + (float) offset.x();
            out.y = ctx.origin().y() + quad.getY(srcIndex) + (float) offset.y();
            out.z = ctx.origin().z() + quad.getZ(srcIndex) + (float) offset.z();

            out.color = colorEncoder.writeColor(ModelQuadUtil.mixARGBColors(colors[srcIndex], quad.getColor(srcIndex)), light.br[srcIndex]);

            out.u = quad.getTexU(srcIndex);
            out.v = quad.getTexV(srcIndex);

            out.light = ModelQuadUtil.mergeBakedLight(quad.getLight(srcIndex), light.lm[srcIndex]);
        }

        var vertexBuffer = builder.getVertexBuffer(normalFace);
        vertexBuffer.push(vertices, material);
    }

    private LightMode getLightingMode(BlockState state, BakedModel model, BlockAndTintGetter world, BlockPos pos, RenderType renderLayer) {
        if (this.useAmbientOcclusion && model.useAmbientOcclusion(state, renderLayer)
                && (((EmbeddiumBakedModelExtension)model).useAmbientOcclusionWithLightEmission(state, renderLayer) || state.getLightEmission(world, pos) == 0)) {
            return LightMode.SMOOTH;
        } else {
            return LightMode.FLAT;
        }
    }
}
