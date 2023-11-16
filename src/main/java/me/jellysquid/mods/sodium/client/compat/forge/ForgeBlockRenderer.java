package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraftforge.common.ForgeConfig;

import java.util.List;

/**
 * Utility class for BlockRenderer, that delegates to the Forge lighting pipeline.
 */
public class ForgeBlockRenderer {
    private static boolean useForgeLightingPipeline = false;
    private static BlockModelRenderer forgeRenderer;

    public static void init() {
        useForgeLightingPipeline = ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
        forgeRenderer = MinecraftClient.getInstance().getBlockRenderManager().getModelRenderer();
    }

    public static boolean useForgeLightingPipeline() {
        return useForgeLightingPipeline;
    }


    private boolean markQuads(ChunkModelBuilder renderData, List<BakedQuad> quads) {
        if (quads.isEmpty()) {
            return true;
        }
        for (int i = 0; i < quads.size(); i++) {
            ModelQuadView src = (ModelQuadView)quads.get(i);
            Sprite sprite = src.getSprite();

            if (sprite != null) {
                renderData.addSprite(sprite);
            }
        }
        return false;
    }

    public boolean renderBlock(LightMode mode, BlockRenderContext ctx, VertexConsumer buffer, MatrixStack stack,
                               Random random, BlockOcclusionCache sideCache, ChunkModelBuilder renderData) {
        if (mode == LightMode.FLAT) {
            forgeRenderer.tesselateWithoutAO(ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), stack, buffer, true, random, ctx.seed(), OverlayTexture.DEFAULT_UV, ctx.modelData(), ctx.renderLayer());
        } else {
            forgeRenderer.tesselateWithAO(ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), stack, buffer, true, random, ctx.seed(), OverlayTexture.DEFAULT_UV, ctx.modelData(), ctx.renderLayer());
        }

        // Process the quads a second time for marking animated sprites and detecting emptiness
        boolean empty;

        random.setSeed(ctx.seed());
        empty = markQuads(renderData, ctx.model().getQuads(ctx.state(), null, random, ctx.modelData(), ctx.renderLayer()));

        for(Direction side : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(ctx.seed());
            empty = markQuads(renderData, ctx.model().getQuads(ctx.state(), side, random, ctx.modelData(), ctx.renderLayer()));
        }
        return !empty;
    }
}
