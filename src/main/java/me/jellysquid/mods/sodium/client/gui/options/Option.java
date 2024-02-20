package me.jellysquid.mods.sodium.client.gui.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

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

    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();
}
