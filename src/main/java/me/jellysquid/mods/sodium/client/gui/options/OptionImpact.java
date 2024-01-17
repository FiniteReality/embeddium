package me.jellysquid.mods.sodium.client.gui.options;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.TranslatableComponent;

public enum OptionImpact {
    LOW(ChatFormatting.GREEN, new TranslatableComponent("sodium.option_impact.low").getString()),
    MEDIUM(ChatFormatting.YELLOW, new TranslatableComponent("sodium.option_impact.medium").getString()),
    HIGH(ChatFormatting.GOLD, new TranslatableComponent("sodium.option_impact.high").getString()),
    EXTREME(ChatFormatting.RED, new TranslatableComponent("sodium.option_impact.extreme").getString()),
    VARIES(ChatFormatting.WHITE, new TranslatableComponent("sodium.option_impact.varies").getString());

    private final ChatFormatting color;
    private final String text;

    OptionImpact(ChatFormatting color, String text) {
        this.color = color;
        this.text = text;
    }

    public String toDisplayString() {
        return this.color + this.text;
    }
}
