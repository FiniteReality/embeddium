package me.jellysquid.mods.sodium.mixin.features.gui;

import com.google.common.base.Strings;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.math.Matrix4f;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraftforge.client.event.CustomizeGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;
import java.util.List;

@Mixin(ForgeGui.class)
public abstract class MixinDebugHud extends Gui {
    @Shadow
    private Font font;

    public MixinDebugHud(Minecraft p_232355_, ItemRenderer p_232356_) {
        super(p_232355_, p_232356_);
    }

    /**
     * @author embeddedt
     * @reason take over rendering of the actual list contents
     */
    @Inject(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z"), locals = LocalCapture.CAPTURE_FAILHARD, cancellable = true, remap = false)
    private void embeddium$renderTextFast(int width, int height, PoseStack poseStack, CallbackInfo ci, ArrayList<String> listL, ArrayList<String> listR, CustomizeGuiOverlayEvent.DebugText event) {
        ci.cancel();

        renderForgeList(poseStack, listL, false);
        renderForgeList(poseStack, listR, true);

        minecraft.getProfiler().pop();
    }

    private void renderForgeList(PoseStack matrixStack, List<String> list, boolean right) {
        renderBackdrop(matrixStack, list, right);
        renderStrings(matrixStack, list, right);
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
        bufferBuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        RenderSystem.setShader(GameRenderer::getPositionColorShader);

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

        BufferBuilder.RenderedBuffer output = bufferBuilder.end();

        BufferUploader.drawWithShader(output);
        RenderSystem.enableTexture();
        RenderSystem.disableBlend();
    }
}