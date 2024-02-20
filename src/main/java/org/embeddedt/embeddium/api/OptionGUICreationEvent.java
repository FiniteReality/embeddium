package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;

import java.util.List;

/**
 * Fired during creation of the main options GUI, to allow adding additional pages. You receive the full list
 * of existing pages as context, so you can insert your page where desired.
 */
public class OptionGUICreationEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionGUICreationEvent> BUS = new EventHandlerRegistrar<>();

    private final List<OptionPage> pages;

    public OptionGUICreationEvent(List<OptionPage> pages) {
        this.pages = pages;
    }

    /**
     * Returns a (mutable) list of the current option pages.
     */
    public List<OptionPage> getPages() {
        return this.pages;
    }
}
