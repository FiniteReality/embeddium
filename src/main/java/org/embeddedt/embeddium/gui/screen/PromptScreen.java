package org.embeddedt.embeddium.gui.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.gui.widgets.AbstractWidget;
import me.jellysquid.mods.sodium.client.gui.widgets.FlatButtonWidget;
import me.jellysquid.mods.sodium.client.util.Dim2i;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.TextComponent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class PromptScreen extends Screen {
    private final Screen prevScreen;
    private final int promptWidth, promptHeight;
    private final Action action;
    private final List<FormattedText> text;
    private FlatButtonWidget closeButton, actionButton;

    public PromptScreen(Screen prev, List<FormattedText> promptText, int promptWidth, int promptHeight, Action action) {
        super(new TextComponent("Prompt"));
        this.prevScreen = prev;
        this.promptWidth = promptWidth;
        this.promptHeight = promptHeight;
        this.text = promptText;
        this.action = action;
    }

    @Override
    public void init(Minecraft minecraft, int width, int height) {
        prevScreen.resize(this.minecraft, this.width, this.height);
        init();
    }

    public void init() {

        int boxX = (prevScreen.width / 2) - (promptWidth / 2);
        int boxY = (prevScreen.height / 2) - (promptHeight / 2);

        this.closeButton = new FlatButtonWidget(new Dim2i((boxX + promptWidth) - 84, (boxY + promptHeight) - 24, 80, 20), new TextComponent("Close"), this::onClose);
        this.closeButton.setStyle(createButtonStyle());

        this.actionButton = new FlatButtonWidget(new Dim2i((boxX + promptWidth) - 198, (boxY + promptHeight) - 24, 110, 20), this.action.label, this::runAction);
        this.actionButton.setStyle(createButtonStyle());

        this.addWidget(this.closeButton);
        this.addWidget(this.actionButton);
    }

    public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
        // First, render the old screen. This gives the illusion of the prompt being on top.
        this.prevScreen.render(matrices, -1, -1, delta);

        matrices.pushPose();
        matrices.translate(0.0f, 0.0f, 1000.0f);

        Gui.fill(matrices, 0, 0, prevScreen.width, prevScreen.height, 0x70090909);

        matrices.translate(0.0f, 0.0f, 50.0f);

        int boxX = (prevScreen.width / 2) - (promptWidth / 2);
        int boxY = (prevScreen.height / 2) - (promptHeight / 2);

        Gui.fill(matrices, boxX, boxY, boxX + promptWidth, boxY + promptHeight, 0xFF171717);

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
                textRenderer.drawShadow(matrices, line, textX, textY, 0xFFFFFFFF);
                textY += textRenderer.lineHeight + 2;
            }

            textY += 8;
        }

        this.closeButton.render(matrices, mouseX, mouseY, delta);
        this.actionButton.render(matrices, mouseX, mouseY, delta);

        super.render(matrices, mouseX, mouseY, delta);

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
