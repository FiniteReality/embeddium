package me.jellysquid.mods.sodium.mixin.core.matrix;

import com.mojang.math.Matrix4f;
import me.jellysquid.mods.sodium.client.render.chunk.GameRendererContext;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GameRenderer.class)
public class GameRendererMixin {
    @Inject(method = "resetProjectionMatrix", at = @At("HEAD"))
    public void captureProjectionMatrix(Matrix4f matrix, CallbackInfo ci) {
        GameRendererContext.PROJECTION_MATRIX = matrix.copy();
    }
}
