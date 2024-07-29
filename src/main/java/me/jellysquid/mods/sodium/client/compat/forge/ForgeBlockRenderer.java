package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.common.ForgeConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import java.util.Random;

/**
 * Utility class for BlockRenderer, that delegates to the Forge lighting pipeline.
 */
public class ForgeBlockRenderer {
    private static boolean useForgeLightingPipeline = false;
    private static ModelBlockRenderer forgeRenderer;

    public static void init() {
        useForgeLightingPipeline = ForgeConfig.CLIENT.experimentalForgeLightPipelineEnabled.get();
        forgeRenderer = Minecraft.getInstance().getBlockRenderer().getModelRenderer();
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
            TextureAtlasSprite sprite = src.getSprite();

            if (sprite != null) {
                renderData.addSprite(sprite);
            }
        }
        return false;
    }

    public boolean renderBlock(LightMode mode, BlockRenderContext ctx, VertexConsumer buffer, PoseStack stack,
                               Random random, BlockOcclusionCache sideCache, ChunkModelBuilder renderData) {
        if (mode == LightMode.FLAT) {
            forgeRenderer.renderModelFlat(ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), stack, buffer, true, random, ctx.seed(), OverlayTexture.NO_OVERLAY, ctx.modelData());
        } else {
            forgeRenderer.renderModelSmooth(ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), stack, buffer, true, random, ctx.seed(), OverlayTexture.NO_OVERLAY, ctx.modelData());
        }

        // Process the quads a second time for marking animated sprites and detecting emptiness
        boolean empty;

        random.setSeed(ctx.seed());
        empty = markQuads(renderData, ctx.model().getQuads(ctx.state(), null, random, ctx.modelData()));

        for(Direction side : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(ctx.seed());
            empty = markQuads(renderData, ctx.model().getQuads(ctx.state(), side, random, ctx.modelData()));
        }
        return !empty;
    }
}
