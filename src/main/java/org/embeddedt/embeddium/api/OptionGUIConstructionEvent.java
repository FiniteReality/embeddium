package org.embeddedt.embeddium.api;

import org.embeddedt.embeddium.api.options.structure.OptionPage;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;

import java.util.List;

/**
 * Fired during creation of the main options GUI, to allow adding additional pages. You receive the full list
 * of existing pages as context, so you can insert your page where desired.
 */
public class OptionGUIConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionGUIConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final List<OptionPage> pages;

    public OptionGUIConstructionEvent(List<OptionPage> pages) {
        this.pages = pages;
    }

    /**
     * Returns a (mutable) list of the current option pages.
     */
    public List<OptionPage> getPages() {
        return this.pages;
    }
    
    public void addPage(OptionPage page) {
        this.pages.add(page);
    }
}
