package me.jellysquid.mods.sodium.mixin.features.render.gui.debug;

import com.google.common.base.Strings;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.MultiBufferSource;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public abstract class ForgeGuiMixin extends GuiComponent {

    @Shadow
    @Final
    private Font font;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "drawGameInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"), cancellable = true)
    private void drawGameInfoFast(PoseStack poseStack, CallbackInfo ci, @Local(ordinal = 0) List<String> list) {
        ci.cancel();

        renderBackdrop(poseStack, list, false);
        renderStrings(poseStack, list, false);
    }

    @Inject(method = "drawSystemInformation", at = @At(value = "INVOKE", target = "Ljava/util/List;size()I"), cancellable = true)
    private void drawSystemInfoFast(PoseStack poseStack, CallbackInfo ci, @Local(ordinal = 0) List<String> list) {
        ci.cancel();

        renderBackdrop(poseStack, list, true);
        renderStrings(poseStack, list, true);
    }

    private void renderStrings(PoseStack matrixStack, List<String> list, boolean right) {
        MultiBufferSource.BufferSource immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());

        Matrix4f positionMatrix = matrixStack.last()
                .pose();

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (!Strings.isNullOrEmpty(string)) {
                int height = 9;
                int width = this.font.width(string);

                float x1 = right ? this.minecraft.getWindow().getGuiScaledWidth() - 2 - width : 2;
                float y1 = 2 + (height * i);

                this.font.drawInBatch(string, x1, y1, 0xe0e0e0, false, positionMatrix, immediate,
                        false, 0, 15728880, this.font.isBidirectional());
            }
        }

        immediate.endBatch();
    }

    private void renderBackdrop(PoseStack matrixStack, List<String> list, boolean right) {
        RenderSystem.enableBlend();
        RenderSystem.disableTexture();
        RenderSystem.defaultBlendFunc();

        int color = 0x90505050;

        float f = (float) (color >> 24 & 255) / 255.0F;
        float g = (float) (color >> 16 & 255) / 255.0F;
        float h = (float) (color >> 8 & 255) / 255.0F;
        float k = (float) (color & 255) / 255.0F;

        BufferBuilder bufferBuilder = Tesselator.getInstance().getBuilder();
        bufferBuilder.begin(GL11.GL_QUADS, DefaultVertexFormat.POSITION_COLOR);

        Matrix4f matrix = matrixStack.last()
                .pose();

        for (int i = 0; i < list.size(); ++i) {
            String string = list.get(i);

            if (Strings.isNullOrEmpty(string)) {
                continue;
            }

            int height = 9;
            int width = this.font.width(string);

            int x = right ? this.minecraft.getWindow().getGuiScaledWidth() - 2 - width : 2;
            int y = 2 + height * i;

            float x1 = x - 1;
            float y1 = y - 1;
            float x2 = x + width + 1;
            float y2 = y + height - 1;

            bufferBuilder.vertex(matrix, x1, y2, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.vertex(matrix, x2, y2, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.vertex(matrix, x2, y1, 0.0F).color(g, h, k, f).endVertex();
            bufferBuilder.vertex(matrix, x1, y1, 0.0F).color(g, h, k, f).endVertex();
        }

        bufferBuilder.end();

        BufferUploader.end(bufferBuilder);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}
