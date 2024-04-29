package org.embeddedt.embeddium.gui.screen;

import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PromptScreen extends Screen {
    private final Screen prevScreen;
    private final int promptWidth, promptHeight;
    private final Action action;
    private final List<FormattedText> text;
    private FlatButtonWidget closeButton, actionButton;

    public PromptScreen(Screen prev, List<FormattedText> promptText, int promptWidth, int promptHeight, Action action) {
        super(Component.literal("Prompt"));
        this.prevScreen = prev;
        this.promptWidth = promptWidth;
        this.promptHeight = promptHeight;
        this.text = promptText;
        this.action = action;
    }

    @Override
    protected void repositionElements() {
        prevScreen.resize(this.minecraft, this.width, this.height);
        super.repositionElements();
    }

    public void init() {

        int boxX = (prevScreen.width / 2) - (promptWidth / 2);
        int boxY = (prevScreen.height / 2) - (promptHeight / 2);

        this.closeButton = new FlatButtonWidget(new Dim2i((boxX + promptWidth) - 84, (boxY + promptHeight) - 24, 80, 20), Component.literal("Close"), this::onClose);
        this.closeButton.setStyle(createButtonStyle());

        this.actionButton = new FlatButtonWidget(new Dim2i((boxX + promptWidth) - 198, (boxY + promptHeight) - 24, 110, 20), this.action.label, this::runAction);
        this.actionButton.setStyle(createButtonStyle());

        this.addRenderableWidget(this.closeButton);
        this.addRenderableWidget(this.actionButton);
    }

    public void render(GuiGraphics drawContext, int mouseX, int mouseY, float delta) {
        // First, render the old screen. This gives the illusion of the prompt being on top.
        this.prevScreen.render(drawContext, -1, -1, delta);

        var matrices = drawContext.pose();
        matrices.pushPose();
        matrices.translate(0.0f, 0.0f, 1000.0f);

        drawContext.fill(0, 0, prevScreen.width, prevScreen.height, 0x70090909);

        matrices.translate(0.0f, 0.0f, 50.0f);

        int boxX = (prevScreen.width / 2) - (promptWidth / 2);
        int boxY = (prevScreen.height / 2) - (promptHeight / 2);

        drawContext.fill(boxX, boxY, boxX + promptWidth, boxY + promptHeight, 0xFF171717);
        drawContext.renderOutline(boxX, boxY, promptWidth, promptHeight, 0xFF121212);

        matrices.translate(0.0f, 0.0f, 50.0f);

        int padding = 5;

        int textX = boxX + padding;
        int textY = boxY + padding;

        int textMaxWidth = promptWidth - (padding * 2);
        int textMaxHeight = promptHeight - (padding * 2);

        var textRenderer = Minecraft.getInstance().font;

        for (var paragraph : this.text) {
            var formatted = textRenderer.split(paragraph, textMaxWidth);

            for (var line : formatted) {
                drawContext.drawString(textRenderer, line, textX, textY, 0xFFFFFFFF, true);
                textY += textRenderer.lineHeight + 2;
            }

            textY += 8;
        }

        super.render(drawContext, mouseX, mouseY, delta);

        matrices.popPose();
    }

    private static FlatButtonWidget.Style createButtonStyle() {
        var style = new FlatButtonWidget.Style();
        style.bgDefault = 0xff2b2b2b;
        style.bgHovered = 0xff393939;
        style.bgDisabled = style.bgDefault;

        style.textDefault = 0xFFFFFFFF;
        style.textDisabled = style.textDefault;

        return style;
    }

    @NotNull
    public List<AbstractWidget> getWidgets() {
        return List.of(this.actionButton, this.closeButton);
    }

    private void runAction() {
        this.action.runnable.run();
        this.onClose();
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(this.prevScreen);
    }

    public record Action(Component label, Runnable runnable) {

    }
}
