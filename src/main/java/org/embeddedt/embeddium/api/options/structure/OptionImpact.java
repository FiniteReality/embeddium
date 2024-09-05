package org.embeddedt.embeddium.api.options.structure;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;

public enum OptionImpact implements TextProvider {
    LOW(ChatFormatting.GREEN, "sodium.option_impact.low"),
    MEDIUM(ChatFormatting.YELLOW, "sodium.option_impact.medium"),
    HIGH(ChatFormatting.GOLD, "sodium.option_impact.high"),
    VARIES(ChatFormatting.WHITE, "sodium.option_impact.varies");

    private final Component text;

    OptionImpact(ChatFormatting color, String text) {
        this.text = Component.translatable(text).withStyle(color);
    }

    @Override
    public Component getLocalizedName() {
        return this.text;
    }
}