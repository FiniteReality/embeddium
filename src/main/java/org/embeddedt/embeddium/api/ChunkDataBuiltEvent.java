package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.embeddedt.embeddium.api.render.chunk.SectionInfoBuilder;

/**
 * This event is fired to allow some control over the chunk render data before it is finalized.
 */
public class ChunkDataBuiltEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<ChunkDataBuiltEvent> BUS = new EventHandlerRegistrar<>();
    private final BuiltSectionInfo.Builder dataBuilder;

    public ChunkDataBuiltEvent(BuiltSectionInfo.Builder dataBuilder) {
        this.dataBuilder = dataBuilder;
    }

    public SectionInfoBuilder getDataBuilder() {
        return this.dataBuilder;
    }
}
