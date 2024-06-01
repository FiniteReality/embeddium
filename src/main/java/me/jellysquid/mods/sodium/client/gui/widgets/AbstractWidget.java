package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;

public abstract class AbstractWidget implements Widget, GuiEventListener, NarratableEntry {
    protected final Font font;
    protected boolean focused;
    protected boolean hovered;

    protected AbstractWidget() {
        this.font = Minecraft.getInstance().font;
    }

    protected void drawString(PoseStack drawContext, String str, int x, int y, int color) {
        Gui.drawString(drawContext, this.font, str, x, y, color);
    }

    protected void drawString(PoseStack drawContext, Component text, int x, int y, int color) {
        Gui.drawString(drawContext, this.font, text, x, y, color);
    }

    public boolean isHovered() {
        return this.hovered;
    }

    protected void drawRect(PoseStack drawContext, int x1, int y1, int x2, int y2, int color) {
        Gui.fill(drawContext, x1, y1, x2, y2, color);
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected int getStringWidth(FormattedText text) {
        return this.font.width(text);
    }

    public NarratableEntry.NarrationPriority narrationPriority() {
        if (this.focused) {
            return NarratableEntry.NarrationPriority.FOCUSED;
        }
        if (this.hovered) {
            return NarratableEntry.NarrationPriority.HOVERED;
        }
        return NarratableEntry.NarrationPriority.NONE;
    }

    @Override
    public void updateNarration(NarrationElementOutput builder) {
        if (this.focused) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.focused"));
        } else if (this.hovered) {
            builder.add(NarratedElementType.USAGE, Component.translatable("narration.button.usage.hovered"));
        }
    }

    public boolean isFocused() {
        return this.focused;
    }

    public void setFocused(boolean focused) {
        this.focused = focused;
    }

    protected void drawBorder(PoseStack drawContext, int x1, int y1, int x2, int y2, int color) {
        Gui.fill(drawContext, x1, y1, x2, y1 + 1, color);
        Gui.fill(drawContext, x1, y2 - 1, x2, y2, color);
        Gui.fill(drawContext, x1, y1, x1 + 1, y2, color);
        Gui.fill(drawContext, x2 - 1, y1, x2, y2, color);
    }

    protected boolean keySelected(int keyCode) {
        return keyCode == InputConstants.KEY_SPACE || keyCode == InputConstants.KEY_RETURN;
    }
}
