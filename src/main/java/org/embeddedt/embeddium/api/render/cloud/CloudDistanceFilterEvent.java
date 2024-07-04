package org.embeddedt.embeddium.api.render.cloud;

import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;

public class CloudDistanceFilterEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<CloudDistanceFilterEvent> BUS = new EventHandlerRegistrar<>();

    private CloudDistanceFilter filter = CloudDistanceFilter.DEFAULT;

    public CloudDistanceFilterEvent() {}

    public CloudDistanceFilter getFilter() {
        return this.filter;
    }

    public void setFilter(CloudDistanceFilter filter) {
        this.filter = filter;
    }
}
