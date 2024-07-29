package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;

public abstract class AbstractWidget implements Widget, GuiEventListener {
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
        return keyCode == GLFW.GLFW_KEY_SPACE || keyCode == GLFW.GLFW_KEY_ENTER;
    }
}
