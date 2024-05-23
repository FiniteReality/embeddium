package org.embeddedt.embeddium.api;

import org.embeddedt.embeddium.api.options.structure.Option;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.embeddedt.embeddium.api.options.OptionIdentifier;

import java.util.List;

/**
 * Fired when an option group is created, to allow replacing options in that group if desired. (Can be used,
 * for instance, to extend the VSync or fullscreen options.)
 */
public class OptionGroupConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionGroupConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final OptionIdentifier<Void> id;
    private final List<Option<?>> options;

    public OptionGroupConstructionEvent(OptionIdentifier<Void> id, List<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public List<Option<?>> getOptions() {
        return this.options;
    }

    public OptionIdentifier<Void> getId() {
        return this.id;
    }
}
