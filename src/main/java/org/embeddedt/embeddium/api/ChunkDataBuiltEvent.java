package org.embeddedt.embeddium.api;

import org.embeddedt.embeddium.api.render.chunk.SectionInfoBuilder;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.jetbrains.annotations.ApiStatus;

/**
 * This event is fired to allow some control over the chunk render data before it is finalized.
 */
@ApiStatus.Internal
public class ChunkDataBuiltEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<ChunkDataBuiltEvent> BUS = new EventHandlerRegistrar<>();
    private final SectionInfoBuilder dataBuilder;

    public ChunkDataBuiltEvent(SectionInfoBuilder dataBuilder) {
        this.dataBuilder = dataBuilder;
    }

    public SectionInfoBuilder getDataBuilder() {
        return this.dataBuilder;
    }
}
