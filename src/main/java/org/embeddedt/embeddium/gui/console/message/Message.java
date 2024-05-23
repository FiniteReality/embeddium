package org.embeddedt.embeddium.gui.console.message;

import net.minecraft.network.chat.Component;

public record Message(MessageLevel level, Component text, double duration) {

}
