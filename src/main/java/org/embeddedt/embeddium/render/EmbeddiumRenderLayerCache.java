package org.embeddedt.embeddium.render;

import com.google.common.collect.ImmutableList;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.FluidState;
import net.minecraft.state.State;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.StampedLock;

/**
 * Implements a caching layer over Forge's predicate logic in {@link RenderLayers}. There is quite a bit
 * of overhead involved in dealing with the arbitrary predicates, so we cache a list of render layers
 * for each state (lazily), and just return that list. The StampedLock we use is not free, but it's
 * much more efficient than Forge's synchronization-based approach.
 */
public class EmbeddiumRenderLayerCache {
    private static final Reference2ReferenceOpenHashMap<RenderLayer, ImmutableList<RenderLayer>> SINGLE_LAYERS = new Reference2ReferenceOpenHashMap<>();
    private static final Reference2ReferenceOpenHashMap<State<?, ?>, ImmutableList<RenderLayer>> LAYERS_BY_STATE = new Reference2ReferenceOpenHashMap<>();
    private static final StampedLock lock = new StampedLock();

    private static <O, S, H extends State<O, S>> ImmutableList<RenderLayer> findExisting(H state) {
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
    public static <O, S, H extends State<O, S>>  ImmutableList<RenderLayer> forState(H state) {
        ImmutableList<RenderLayer> list = findExisting(state);

        if(list == null) {
            list = createList(state);
        }

        return list;
    }

    private static <O, S, H extends State<O, S>> ImmutableList<RenderLayer> createList(H state) {
        List<RenderLayer> foundLayers = new ArrayList<>(2);
        if(state instanceof BlockState) {
            BlockState blockState = (BlockState)state;
            for(RenderLayer layer : RenderLayer.getBlockLayers()) {
                if(RenderLayers.canRenderInLayer(blockState, layer)) {
                    foundLayers.add(layer);
                }
            }
        } else if(state instanceof FluidState) {
            FluidState fluidState = (FluidState)state;
            for(RenderLayer layer : RenderLayer.getBlockLayers()) {
                if(RenderLayers.canRenderInLayer(fluidState, layer)) {
                    foundLayers.add(layer);
                }
            }
        } else {
            throw new IllegalArgumentException("Unexpected type of state received: " + state.getClass().getName());
        }

        ImmutableList<RenderLayer> layerList;

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
        for(RenderLayer layer : RenderLayer.getBlockLayers()) {
            SINGLE_LAYERS.put(layer, ImmutableList.of(layer));
        }
    }
}
