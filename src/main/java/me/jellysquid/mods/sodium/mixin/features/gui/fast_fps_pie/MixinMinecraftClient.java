package me.jellysquid.mods.sodium.mixin.features.gui.fast_fps_pie;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.Tesselator;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.profiling.ProfileResults;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MixinMinecraftClient {
    @Shadow
    @Final
    public Font font;

    private MultiBufferSource.BufferSource immediate;

    @Inject(method = "renderFpsMeter", at = @At(value = "INVOKE", ordinal = 0, target = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I"))
    private void preRenderText(PoseStack matrices, ProfileResults profileResult, CallbackInfo ci) {
        this.immediate = MultiBufferSource.immediate(Tesselator.getInstance().getBuilder());
    }

    @Redirect(method = "renderFpsMeter", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;drawShadow(Lcom/mojang/blaze3d/vertex/PoseStack;Ljava/lang/String;FFI)I"))
    private int drawWithShadow(Font textRenderer, PoseStack matrices, String text, float x, float y, int color) {
        if (text != null) {
            return this.font.drawInBatch(text, x, y, color, true, matrices.last().pose(), this.immediate,
                    false, 0, 15728880, this.font.isBidirectional());
        }
        return 0;
    }

    @Inject(method = "renderFpsMeter", at = @At("TAIL"))
    private void renderText(PoseStack matrices, ProfileResults profileResult, CallbackInfo ci) {
        this.immediate.endBatch();
    }
}