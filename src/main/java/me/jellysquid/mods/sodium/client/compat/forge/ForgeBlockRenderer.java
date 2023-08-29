package me.jellysquid.mods.sodium.client.compat.forge;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
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

import java.util.List;

/**
 * Utility class for BlockRenderer, that uses the Forge lighting pipeline.
 */
public class ForgeBlockRenderer {
    private final BlockColors colors = MinecraftClient.getInstance().getBlockColors();
    private final ThreadLocal<QuadLighter> lighterFlat = ThreadLocal.withInitial(() -> new FlatQuadLighter(colors));
    private final ThreadLocal<QuadLighter> lighterSmooth = ThreadLocal.withInitial(() -> new SmoothQuadLighter(colors));

    private void processQuad(ChunkModelBuilder renderData, BakedQuad quad) {
        ModelQuadView src = (ModelQuadView)quad;
        Sprite sprite = src.getSprite();

        if (sprite != null) {
            renderData.addSprite(sprite);
        }
    }

    public boolean renderBlock(LightMode mode, BlockRenderContext ctx, VertexConsumer buffer, MatrixStack stack,
                               Random random, BlockOcclusionCache sideCache, ChunkModelBuilder renderData) {
        BlockState state = ctx.state();
        BakedModel model = ctx.model();
        ModelData data = ctx.modelData();
        RenderLayer layer = ctx.renderLayer();
        BlockRenderView world = ctx.localSlice();
        BlockPos pos = ctx.pos();

        QuadLighter lighter = mode == LightMode.FLAT ? this.lighterFlat.get() : this.lighterSmooth.get();
        QuadLighter flatLighter = null;
        MatrixStack.Entry matrixEntry = stack.peek();

        // render
        boolean empty = true;
        random.setSeed(ctx.seed());

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
            random.setSeed(ctx.seed());
            quads = model.getQuads(state, side, random, data, layer);
            if(!quads.isEmpty())
            {
                if(sideCache.shouldDrawSide(state, world, pos, side))
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
