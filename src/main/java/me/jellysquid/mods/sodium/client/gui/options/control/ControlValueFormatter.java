package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.network.chat.TranslatableComponent;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? new TranslatableComponent("options.guiScale.auto").getString() : new TranslatableComponent(v + "x").getString();
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? new TranslatableComponent("options.framerateLimit.max").getString() : new TranslatableComponent("options.framerate", v).getString();
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return new TranslatableComponent("options.gamma.min").getString();
            } else if (v == 100) {
                return new TranslatableComponent("options.gamma.max").getString();
            } else {
                return new TranslatableComponent(v + "%").getString();
            }
        };
    }

    String format(int value);

    static ControlValueFormatter percentage() {
        return (v) -> new TranslatableComponent(v + "%").getString();
    }

    static ControlValueFormatter multiplier() {
        return (v) -> new TranslatableComponent(v + "x").getString();
    }

    static ControlValueFormatter quantity(String name) {
        return (v) -> new TranslatableComponent(name, v).getString();
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> new TranslatableComponent(v == 0 ? disableText : name, v).getString();
    }

    static ControlValueFormatter number() {
        return String::valueOf;
    }
}
