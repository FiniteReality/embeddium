package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is fired to allow some control over the chunk render data before it is finalized.
 */
@ApiStatus.Internal
public class ChunkDataBuiltEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<ChunkDataBuiltEvent> BUS = new EventHandlerRegistrar<>();
    private final ChunkRenderData.Builder dataBuilder;

    public ChunkDataBuiltEvent(ChunkRenderData.Builder dataBuilder) {
        this.dataBuilder = dataBuilder;
    }

    public ChunkRenderData.Builder getDataBuilder() {
        return this.dataBuilder;
    }
}
