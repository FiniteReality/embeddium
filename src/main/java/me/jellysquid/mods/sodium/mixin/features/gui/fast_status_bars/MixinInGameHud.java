package me.jellysquid.mods.sodium.mixin.features.gui.fast_status_bars;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Matrix4f;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public abstract class MixinInGameHud extends GuiComponent {
    @Shadow
    protected abstract Player getCameraPlayer();

    private final BufferBuilder bufferBuilder = new BufferBuilder(8192);
    // It's possible for status bar rendering to be skipped
    private boolean isRenderingStatusBars;

    @Inject(method = "renderPlayerHealth", at = @At("HEAD"))
    private void preRenderStatusBars(PoseStack matrices, CallbackInfo ci) {
        if (this.getCameraPlayer() != null) {
            this.bufferBuilder.begin(4, DefaultVertexFormat.POSITION_TEX);
            this.isRenderingStatusBars = true;
        } else {
            this.isRenderingStatusBars = false;
        }
    }

    @Redirect(method = { "renderPlayerHealth", "drawHeart" }, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Gui;blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIII)V"))
    private void drawTexture(Gui inGameHud, PoseStack matrices, int x0, int y0, int u, int v, int width, int height) {
        Matrix4f matrix = matrices.last().pose();
        int x1 = x0 + width;
        int y1 = y0 + height;
        int z = this.getBlitOffset();
        // Default texture size is 256x256
        float u0 = u / 256f;
        float u1 = (u + width) / 256f;
        float v0 = v / 256f;
        float v1 = (v + height) / 256f;

        this.bufferBuilder.vertex(matrix, x0, y1, z).uv(u0, v1).endVertex();
        this.bufferBuilder.vertex(matrix, x1, y1, z).uv(u1, v1).endVertex();
        this.bufferBuilder.vertex(matrix, x1, y0, z).uv(u1, v0).endVertex();
        this.bufferBuilder.vertex(matrix, x0, y0, z).uv(u0, v0).endVertex();
    }

    @Inject(method = "renderPlayerHealth", at = @At("RETURN"))
    private void renderStatusBars(PoseStack matrices, CallbackInfo ci) {
        if (this.isRenderingStatusBars) {            
            this.bufferBuilder.end();
            BufferUploader.end(this.bufferBuilder);
        }
    }
}