package org.embeddedt.embeddium.api;

import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.BlockRenderView;
import net.minecraftforge.client.model.data.ModelData;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Allows registering custom block renderers.
 * <p></p>
 * This API is designed to minimize allocation rate and overhead for most blocks while
 * allowing full control over the pipeline where needed. Register a {@link RenderPopulator}
 * that will be called during meshing to determine if the block should be rendered by a
 * custom renderer. Only add your {@link Renderer} instance if you actually want to render
 * the block.
 */
public class BlockRendererRegistry {
    private static final BlockRendererRegistry instance = new BlockRendererRegistry();
    private final List<RenderPopulator> renderPopulators = new CopyOnWriteArrayList<>();
    private BlockRendererRegistry() {}

    public static BlockRendererRegistry instance() {
        return instance;
    }

    /**
     * Register a new render populator to be used during chunk meshing.
     */
    public void registerRenderPopulator(RenderPopulator populator) {
        renderPopulators.add(populator);
    }

    /**
     * Get a list of custom renderers for the given block & context.
     */
    public void fillCustomRenderers(List<Renderer> resultList, BlockState state, BlockPos pos, BlockRenderView world, RenderLayer layer) {
        if(renderPopulators.isEmpty())
            return;

        for(RenderPopulator populator : renderPopulators) {
            populator.fillCustomRenderers(resultList, state, pos, world, layer);
        }
    }

    public enum RenderResult {
        /**
         * Return this if you have handled all rendering for the block, and it should not be passed to other renderers.
         */
        OVERRIDE,
        /**
         * Return this if you want the block to still be rendered using the default renderer and/or any other registered
         * renderers.
         */
        PASS
    }

    public interface RenderPopulator {
        void fillCustomRenderers(List<Renderer> resultList, BlockState state, BlockPos pos, BlockRenderView world, RenderLayer layer);

        static RenderPopulator forRenderer(Renderer renderer) {
            return (resultList, state, pos, world, layer) -> resultList.add(renderer);
        }
    }

    public interface Renderer {
        /**
         * Provides the opportunity to render a block in the subchunk using the given {@link VertexConsumer}.
         *
         * @param state the state of the block
         * @param pos the position of the block in the world
         * @param world the thread-safe subchunk access
         * @param consumer the vertex consumer to add block meshes to
         * @param random the RNG used for rendering
         * @param modelData the model data for this block
         * @return the result of the rendering
         */
        RenderResult renderBlock(BlockState state,
                BlockPos pos,
                BlockRenderView world,
                VertexConsumer consumer,
                Random random,
                ModelData modelData,
                RenderLayer layer
        );
    }
}
