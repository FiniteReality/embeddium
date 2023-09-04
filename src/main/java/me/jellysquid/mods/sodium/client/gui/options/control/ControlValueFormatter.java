package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? new TranslatableText("options.guiScale.auto") : new TranslatableText(v + "x");
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? new TranslatableText("options.framerateLimit.max") : new TranslatableText("options.framerate", v);
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return new TranslatableText("options.gamma.min");
            } else if (v == 100) {
                return new TranslatableText("options.gamma.max");
            } else {
                return new TranslatableText(v + "%");
            }
        };
    }

    Text format(int value);

    static ControlValueFormatter percentage() {
        return (v) -> new TranslatableText(v + "%");
    }

    static ControlValueFormatter multiplier() {
        return (v) -> new TranslatableText(v + "x");
    }

    static ControlValueFormatter quantity(String name) {
        return (v) -> new TranslatableText(name, v);
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> new TranslatableText(v == 0 ? disableText : name, v);
    }

    static ControlValueFormatter number() {
        return (v) -> new TranslatableText(String.valueOf(v));
    }
}
