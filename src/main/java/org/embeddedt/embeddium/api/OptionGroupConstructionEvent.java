package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;

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

    private final ResourceLocation id;
    private final List<Option<?>> options;

    public OptionGroupConstructionEvent(ResourceLocation id, List<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public ResourceLocation getId() {
        return this.id;
    }

    /**
     * Call to replace some or all options in the group with new instances.
     * You should check if the option group ID matches what you expect first, to avoid iterating options unnecessarily.
     * @param newOption a new option to use in place of the old one
     */
    public void replaceOption(Option<?> newOption) {
        ResourceLocation replacingId = newOption.getReplacedId();
        Objects.requireNonNull(replacingId, "Option does not replace any other option");
        options.replaceAll(option -> {
            if(replacingId.equals(option.getId())) {
                return newOption;
            } else {
                return option;
            }
        });
    }
}
