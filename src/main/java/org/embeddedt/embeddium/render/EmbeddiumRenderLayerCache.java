package org.embeddedt.embeddium.render;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateHolder;
import net.minecraft.world.level.material.FluidState;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;

/**
 * Implements a caching layer over Forge's predicate logic in {@link ItemBlockRenderTypes}. There is quite a bit
 * of overhead involved in dealing with the arbitrary predicates, so we cache a list of render layers
 * for each state (lazily), and just return that list. The StampedLock we use is not free, but it's
 * much more efficient than Forge's synchronization-based approach.
 */
public class EmbeddiumRenderLayerCache {
    private static final boolean DISABLE_CACHE = Boolean.getBoolean("embeddium.disableRenderLayerCache");
    private static final Reference2ReferenceOpenHashMap<RenderType, ImmutableList<RenderType>> SINGLE_LAYERS = new Reference2ReferenceOpenHashMap<>();
    private static final Reference2ReferenceOpenHashMap<StateHolder<?, ?>, ImmutableList<RenderType>> LAYERS_BY_STATE = new Reference2ReferenceOpenHashMap<>();
    private static final StampedLock lock = new StampedLock();

    private static <O, S, H extends StateHolder<O, S>> ImmutableList<RenderType> findExisting(H state) {
        long stamp = lock.readLock();

        try {
            return LAYERS_BY_STATE.get(state);
        } finally {
            lock.unlock(stamp);
        }
    }

    /**
     * Retrieve the list of render layers for the given block/fluid state.
     * @param state a BlockState or FluidState
     * @return a list of render layers that the block/fluid state should be rendered on
     */
    public static <O, S, H extends StateHolder<O, S>>  List<RenderType> forState(H state) {
        if(DISABLE_CACHE) {
            return generateList(state);
        }

        ImmutableList<RenderType> list = findExisting(state);

        if(list == null) {
            list = createList(state);
        }

        return list;
    }

    private static <O, S, H extends StateHolder<O, S>> List<RenderType> generateList(H state) {
        List<RenderType> foundLayers = new ArrayList<>(2);
        if(state instanceof BlockState) {
            BlockState blockState = (BlockState)state;
            for(RenderType layer : RenderType.chunkBufferLayers()) {
                if(ItemBlockRenderTypes.canRenderInLayer(blockState, layer)) {
                    foundLayers.add(layer);
                }
            }
        } else if(state instanceof FluidState) {
            FluidState fluidState = (FluidState)state;
            for(RenderType layer : RenderType.chunkBufferLayers()) {
                if(ItemBlockRenderTypes.canRenderInLayer(fluidState, layer)) {
                    foundLayers.add(layer);
                }
            }
        } else {
            throw new IllegalArgumentException("Unexpected type of state received: " + state.getClass().getName());
        }

        return foundLayers;
    }

    private static <O, S, H extends StateHolder<O, S>> ImmutableList<RenderType> createList(H state) {
        List<RenderType> foundLayers = generateList(state);

        ImmutableList<RenderType> layerList;

        // Deduplicate simple lists
        if(foundLayers.isEmpty()) {
            layerList = ImmutableList.of();
        } else if(foundLayers.size() == 1) {
            layerList = SINGLE_LAYERS.get(foundLayers.get(0));
            Objects.requireNonNull(layerList);
        } else {
            layerList = ImmutableList.copyOf(foundLayers);
        }

        long stamp = lock.writeLock();
        try {
            LAYERS_BY_STATE.put(state, layerList);
        } finally {
            lock.unlock(stamp);
        }

        return layerList;
    }

    /**
     * Invalidate the cached mapping of states to render layers to force the data to be queried
     * from Forge again.
     */
    public static void invalidate() {
        long stamp = lock.writeLock();
        try {
            LAYERS_BY_STATE.clear();
        } finally {
            lock.unlock(stamp);
        }
    }

    static {
        for(RenderType layer : RenderType.chunkBufferLayers()) {
            SINGLE_LAYERS.put(layer, ImmutableList.of(layer));
        }
        if(DISABLE_CACHE) {
            SodiumClientMod.logger().warn("Render layer cache is disabled, performance will be affected.");
        }
    }
}
