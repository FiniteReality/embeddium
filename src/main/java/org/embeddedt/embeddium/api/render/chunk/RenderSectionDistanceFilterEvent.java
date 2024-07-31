package org.embeddedt.embeddium.api.render.chunk;

import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;

/**
 * Fired to allow mods to adjust the function used for culling sections outside of render distance.
 * The event is currently only fired once for performance reasons.
 */
public class RenderSectionDistanceFilterEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<RenderSectionDistanceFilterEvent> BUS = new EventHandlerRegistrar<>();

    private RenderSectionDistanceFilter filter = RenderSectionDistanceFilter.DEFAULT;

    public RenderSectionDistanceFilterEvent() {}

    public RenderSectionDistanceFilter getFilter() {
        return this.filter;
    }

    /**
     * Set a new render distance filter. Not delegating to the previous filter may cause other mods' behavior to be
     * silently overwritten.
     */
    public void setFilter(RenderSectionDistanceFilter filter) {
        this.filter = filter;
    }
}
