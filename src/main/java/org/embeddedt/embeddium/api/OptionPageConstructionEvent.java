package org.embeddedt.embeddium.api;

import org.embeddedt.embeddium.api.options.structure.OptionGroup;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.embeddedt.embeddium.api.options.OptionIdentifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fired when an option page is created, to allow adding additional {@link OptionGroup} entries at the end of the page.
 */
public class OptionPageConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionPageConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final OptionIdentifier<Void> id;
    private final Component name;
    private final List<OptionGroup> additionalGroups = new ArrayList<>();

    public OptionPageConstructionEvent(OptionIdentifier<Void> id, Component name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the ID of the option group.
     */
    public OptionIdentifier<Void> getId() {
        return this.id;
    }

    /**
     * Returns the translatable name of the option group.
     */
    public Component getName() {
        return this.name;
    }

    /**
     * Add a new option group to the end of this page. The group will be inserted at the end, after any
     * existing groups.
     */
    public void addGroup(OptionGroup group) {
        this.additionalGroups.add(group);
    }

    public List<OptionGroup> getAdditionalGroups() {
        return Collections.unmodifiableList(this.additionalGroups);
    }
}
