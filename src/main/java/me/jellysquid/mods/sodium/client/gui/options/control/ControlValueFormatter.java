package me.jellysquid.mods.sodium.client.gui.options.control;

import net.minecraft.network.chat.Component;

public interface ControlValueFormatter {
    static ControlValueFormatter guiScale() {
        return (v) -> (v == 0) ? Component.translatable("options.guiScale.auto").getString() : v + "x";
    }

    static ControlValueFormatter fpsLimit() {
        return (v) -> (v == 260) ? Component.translatable("options.framerateLimit.max").getString() : Component.translatable("options.framerate", v).getString();
    }

    static ControlValueFormatter brightness() {
        return (v) -> {
            if (v == 0) {
                return Component.translatable("options.gamma.min").getString();
            } else if (v == 100) {
                return Component.translatable("options.gamma.max").getString();
            } else {
                return v + "%";
            }
        };
    }

    static ControlValueFormatter biomeBlend() {
        return (v) -> (v == 0) ? Component.translatable("gui.none").getString() : Component.translatable("sodium.options.biome_blend.value", v).getString();
    }

    String format(int value);

    static ControlValueFormatter translateVariable(String key) {
        return (v) -> Component.translatable(key, v).getString();
    }

    static ControlValueFormatter percentage() {
        return (v) -> v + "%";
    }

    static ControlValueFormatter multiplier() {
        return (v) -> v + "x";
    }

    static ControlValueFormatter quantityOrDisabled(String name, String disableText) {
        return (v) -> v == 0 ? disableText : v + " " + name;
    }

    static ControlValueFormatter number() {
        return String::valueOf;
    }
}