package org.embeddedt.embeddium.gui.options;

import org.embeddedt.embeddium.gui.options.control.Control;
import org.embeddedt.embeddium.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.gui.options.OptionIdentifier;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface Option<T> {
    @Nullable
    default OptionIdentifier<T> getId() {
        return null;
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
