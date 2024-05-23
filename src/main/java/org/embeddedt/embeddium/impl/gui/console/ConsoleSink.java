package org.embeddedt.embeddium.impl.gui.console;

import org.embeddedt.embeddium.impl.gui.console.message.MessageLevel;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

public interface ConsoleSink {
    void logMessage(@NotNull MessageLevel level, @NotNull Component text, double duration);
}
