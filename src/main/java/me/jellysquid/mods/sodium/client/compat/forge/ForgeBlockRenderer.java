package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.lighting.FlatQuadLighter;
import net.minecraftforge.client.model.lighting.QuadLighter;
import net.minecraftforge.client.model.lighting.SmoothQuadLighter;
import net.minecraftforge.common.ForgeConfig;

import java.util.List;

/**
 * Utility class for BlockRenderer, that uses the Forge lighting pipeline.
 */
public class ForgeBlockRenderer {
    private final BlockColors colors = MinecraftClient.getInstance().getBlockColors();
    private final ThreadLocal<QuadLighter> lighterFlat = ThreadLocal.withInitial(() -> new FlatQuadLighter(colors));
    private final ThreadLocal<QuadLighter> lighterSmooth = ThreadLocal.withInitial(() -> new SmoothQuadLighter(colors));

    private static boolean useForgeLightingPipeline = false;

    public static void init() {
        useForgeLightingPipeline = ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
    }

    public static boolean useForgeLightingPipeline() {
        return useForgeLightingPipeline;
    }

    private void processQuad(ChunkModelBuilder renderData, BakedQuad quad) {
        ModelQuadView src = (ModelQuadView)quad;
        Sprite sprite = src.getSprite();

        if (sprite != null) {
            renderData.addSprite(sprite);
        }
    }

    public boolean renderBlock(LightMode mode, BlockState state, BlockPos pos, BlockRenderView world, BakedModel model, MatrixStack stack,
                               VertexConsumer buffer, Random random, long seed, ModelData data, boolean checkSides, BlockOcclusionCache sideCache,
                               ChunkModelBuilder renderData, RenderLayer layer) {
        QuadLighter lighter = mode == LightMode.FLAT ? this.lighterFlat.get() : this.lighterSmooth.get();
        QuadLighter flatLighter = null;
        MatrixStack.Entry matrixEntry = stack.peek();

        // render
        boolean empty = true;
        random.setSeed(seed);

        List<BakedQuad> quads = model.getQuads(state, null, random, data, layer);
        if(!quads.isEmpty()) {
            lighter.setup(world, pos, state);
            empty = false;
            // noinspection ForLoopReplaceableByForEach
            for(int i = 0; i < quads.size(); i++) {
                BakedQuad quad = quads.get(i);
                if(mode == LightMode.SMOOTH && !quad.hasAmbientOcclusion()) {
                    if(flatLighter == null) {
                        flatLighter = this.lighterFlat.get();
                        flatLighter.setup(world, pos, state);
                    }
                    flatLighter.process(buffer, matrixEntry, quad, OverlayTexture.DEFAULT_UV);
                } else
                    lighter.process(buffer, matrixEntry, quad, OverlayTexture.DEFAULT_UV);
                processQuad(renderData, quad);
            }
        }

        for(Direction side : DirectionUtil.ALL_DIRECTIONS)
        {
            random.setSeed(seed);
            quads = model.getQuads(state, side, random, data, layer);
            if(!quads.isEmpty())
            {
                if(!checkSides || sideCache.shouldDrawSide(state, world, pos, side))
                {
                    if(empty) lighter.setup(world, pos, state);
                    empty = false;
                    // noinspection ForLoopReplaceableByForEach
                    for(int i = 0; i < quads.size(); i++) {
                        BakedQuad quad = quads.get(i);
                        if(mode == LightMode.SMOOTH && !quad.hasAmbientOcclusion()) {
                            if(flatLighter == null) {
                                flatLighter = this.lighterFlat.get();
                                flatLighter.setup(world, pos, state);
                            }
                            flatLighter.process(buffer, matrixEntry, quad, OverlayTexture.DEFAULT_UV);
                        } else
                            lighter.process(buffer, matrixEntry, quad, OverlayTexture.DEFAULT_UV);
                        processQuad(renderData, quad);
                    }
                }
            }
        }
        lighter.reset();
        if(flatLighter != null)
            flatLighter.reset();
        return !empty;
    }
}
