package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.block.BlockModelRenderer;
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

    public boolean renderBlock(LightMode mode, BlockState state, BlockPos pos, BlockRenderView world, BakedModel model, MatrixStack stack,
                               VertexConsumer buffer, Random random, long seed, ModelData data, boolean checkSides, BlockOcclusionCache sideCache,
                               ChunkModelBuilder renderData, RenderLayer layer) {
        if (mode == LightMode.FLAT) {
            forgeRenderer.tesselateWithoutAO(world, model, state, pos, stack, buffer, checkSides, random, seed, 0, data, layer);
        } else {
            forgeRenderer.tesselateWithAO(world, model, state, pos, stack, buffer, checkSides, random, seed, 0, data, layer);
        }

        // Process the quads a second time for marking animated sprites and detecting emptiness
        boolean empty;

        random.setSeed(seed);
        empty = markQuads(renderData, model.getQuads(state, null, random, data, layer));

        for(Direction side : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(seed);
            empty = markQuads(renderData, model.getQuads(state, side, random, data, layer));
        }
        return !empty;
    }
}
