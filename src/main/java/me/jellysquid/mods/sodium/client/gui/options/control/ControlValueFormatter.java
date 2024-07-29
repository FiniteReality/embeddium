package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? new TranslatableComponent("options.guiScale.auto") : new TextComponent(v + "x");
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? new TranslatableComponent("options.framerateLimit.max") : new TranslatableComponent("options.framerate", v);
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return new TranslatableComponent("options.gamma.min");
            } else if (v == 100) {
                return new TranslatableComponent("options.gamma.max");
            } else {
                return new TextComponent(v + "%");
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? new TranslatableComponent("gui.none") : new TranslatableComponent("sodium.options.biome_blend.value", v);
    }

    Component format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> new TranslatableComponent(key, v);
    }

    static ControlValueFormatter percentage() {
        return (v) -> new TextComponent(v + "%");
    }

    static ControlValueFormatter multiplier() {
        return (v) -> new TextComponent(v + "x");
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> new TextComponent(v == 0 ? disableText : v + " " + name);
    }

    static ControlValueFormatter number() {
        return (v) -> new TextComponent(String.valueOf(v));
    }
}
