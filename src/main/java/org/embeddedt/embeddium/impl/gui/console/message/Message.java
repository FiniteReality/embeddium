package org.embeddedt.embeddium.impl.gui.console.message;

import net.minecraft.network.chat.Component;

public record Message(MessageLevel level, Component text, double duration) {

}
