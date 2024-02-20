package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Fired when an option page is created, to allow adding additional {@link OptionGroup} entries at the end of the page.
 */
public class OptionPageConstructionEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<OptionPageConstructionEvent> BUS = new EventHandlerRegistrar<>();

    private final ResourceLocation id;
    private final Component name;
    private final List<OptionGroup> additionalGroups = new ArrayList<>();

    public OptionPageConstructionEvent(ResourceLocation id, Component name) {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the ID of the option group.
     */
    public ResourceLocation getId() {
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
