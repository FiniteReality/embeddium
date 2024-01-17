package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.occlusion.BlockOcclusionCache;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.lighting.FlatQuadLighter;
import net.minecraftforge.client.model.lighting.QuadLighter;
import net.minecraftforge.client.model.lighting.SmoothQuadLighter;
import net.minecraftforge.common.ForgeConfig;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;

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

    public boolean renderBlock(LightMode mode, BlockState state, BlockPos pos, BlockAndTintGetter world, BakedModel model, PoseStack stack,
                               VertexConsumer buffer, RandomSource random, long seed, ModelData data, boolean checkSides, BlockOcclusionCache sideCache,
                               ChunkModelBuilder renderData, RenderType layer) {
        if (mode == LightMode.FLAT) {
            forgeRenderer.tesselateWithoutAO(world, model, state, pos, stack, buffer, checkSides, random, seed, OverlayTexture.NO_OVERLAY, data, layer);
        } else {
            forgeRenderer.tesselateWithAO(world, model, state, pos, stack, buffer, checkSides, random, seed, OverlayTexture.NO_OVERLAY, data, layer);
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
