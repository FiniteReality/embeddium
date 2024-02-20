package me.jellysquid.mods.sodium.client.gui.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Option<T> {
    ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    default ResourceLocation getId() {
        return DEFAULT_ID;
    }

    Component getName();

    Component getTooltip();

    OptionImpact getImpact();

    Control<T> getControl();

    T getValue();

    void setValue(T value);

    void reset();

    OptionStorage<?> getStorage();

    boolean isAvailable();

    /**
     * @return true if the option has a value change waiting to be applied by clicking "Applied"
     */
    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();

    /**
     * Return the ID of the option this option replaces.
     */
    @Nullable
    default ResourceLocation getReplacedId() {
        return null;
    }

    /**
     * @return true if this option should be shown on screen
     */
    default boolean isVisible() {
        return true;
    }

    /**
     * Returns true if setValue was called since a call to this function. Clears its flag after being called.
     */
    default boolean hasChangedSinceLastPoll() {
        return false;
    }
}
