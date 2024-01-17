package me.jellysquid.mods.sodium.client.gui.widgets;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.Widget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.opengl.GL20C;

import java.util.function.Consumer;

public abstract class AbstractWidget implements Widget, GuiEventListener {
    protected final Font font;

    protected AbstractWidget() {
        this.font = Minecraft.getInstance().font;
    }

    protected void drawString(PoseStack matrixStack, String str, int x, int y, int color) {
        this.font.draw(matrixStack, str, x, y, color);
    }

    protected void drawText(PoseStack matrixStack, Component text, int x, int y, int color) {
        this.font.draw(matrixStack, text, x, y, color);
    }

    protected void drawRect(double x1, double y1, double x2, double y2, int color) {
        float a = (float) (color >> 24 & 255) / 255.0F;
        float r = (float) (color >> 16 & 255) / 255.0F;
        float g = (float) (color >> 8 & 255) / 255.0F;
        float b = (float) (color & 255) / 255.0F;

        this.drawQuads(vertices -> addQuad(vertices, x1, y1, x2, y2, a, r, g, b));
    }

    protected void drawQuads(Consumer<VertexConsumer> consumer) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(GL20C.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);

        consumer.accept(bufferBuilder);

        bufferBuilder.end();

        BufferUploader.end(bufferBuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }

    protected static void addQuad(VertexConsumer consumer, double x1, double y1, double x2, double y2, float a, float r, float g, float b) {
        consumer.vertex(x2, y1, 0.0D).color(r, g, b, a).endVertex();
        consumer.vertex(x1, y1, 0.0D).color(r, g, b, a).endVertex();
        consumer.vertex(x1, y2, 0.0D).color(r, g, b, a).endVertex();
        consumer.vertex(x2, y2, 0.0D).color(r, g, b, a).endVertex();
    }

    protected void playClickSound() {
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
    }

    protected int getStringWidth(String text) {
        return this.font.width(text);
    }

    protected int getTextWidth(Component text) {
        return this.font.width(text);
    }
}
