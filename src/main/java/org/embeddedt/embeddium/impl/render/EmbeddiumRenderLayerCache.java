package org.embeddedt.embeddium.impl.render;

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

/**
 * Implements a caching layer over Forge's predicate logic in {@link ItemBlockRenderTypes}. There is quite a bit
 * of overhead involved in dealing with the arbitrary predicates, so we cache a list of render layers
 * for each state (lazily), and just return that list.
 */
public class EmbeddiumRenderLayerCache {
    private static final boolean DISABLE_CACHE = Boolean.getBoolean("embeddium.disableRenderLayerCache");
    private static final Reference2ReferenceOpenHashMap<RenderType, ImmutableList<RenderType>> SINGLE_LAYERS = new Reference2ReferenceOpenHashMap<>();

    private final Reference2ReferenceOpenHashMap<StateHolder<?, ?>, ImmutableList<RenderType>> stateToLayerMap = new Reference2ReferenceOpenHashMap<>();

    public EmbeddiumRenderLayerCache() {
    }

    /**
     * Retrieve the list of render layers for the given block/fluid state.
     * @param state a BlockState or FluidState
     * @return a list of render layers that the block/fluid state should be rendered on
     */
    public <O, S, H extends StateHolder<O, S>>  List<RenderType> forState(H state) {
        if(DISABLE_CACHE) {
            return generateList(state);
        }

        ImmutableList<RenderType> list = stateToLayerMap.get(state);

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

    private <O, S, H extends StateHolder<O, S>> ImmutableList<RenderType> createList(H state) {
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

        stateToLayerMap.put(state, layerList);

        return layerList;
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
