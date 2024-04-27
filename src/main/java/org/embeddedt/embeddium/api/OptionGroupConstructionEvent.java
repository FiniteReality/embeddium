package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;

import java.util.List;
import java.util.Objects;

/**
 * Fired when an option group is created, to allow replacing options in that group if desired. (Can be used,
 * for instance, to extend the VSync or fullscreen options.)
 *
 * Adding new options to a group is not allowed, you must use {@link OptionPageConstructionEvent} and create
 * a new group.
 */
public class OptionGroupConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionGroupConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final OptionIdentifier<Void> id;
    private final List<Option<?>> options;

    public OptionGroupConstructionEvent(OptionIdentifier<Void> id, List<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public OptionIdentifier<Void> getId() {
        return this.id;
    }
}
