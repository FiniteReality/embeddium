package me.jellysquid.mods.sodium.mixin.features.render.world.clouds;

import me.jellysquid.mods.sodium.client.render.immediate.CloudRenderer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.resource.ResourceManager;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = WorldRenderer.class, priority = 990)
public class WorldRendererMixin {
    @Shadow
    private @Nullable ClientWorld world;
    @Shadow
    private int ticks;

    @Shadow
    @Final
    private MinecraftClient client;

    @Unique
    private CloudRenderer cloudRenderer;

    /**
     * @author jellysquid3
     * @reason Optimize cloud rendering
     */
    @Overwrite
    public void renderClouds(MatrixStack matrices, Matrix4f projectionMatrix, float tickDelta, double x, double y, double z) {
        if (!this.world.getDimensionEffects().renderClouds(this.world, this.ticks, tickDelta, matrices, x, y, z, projectionMatrix)) {
            if (this.cloudRenderer == null) {
                this.cloudRenderer = new CloudRenderer(this.client.getResourceManager());
            }

            this.cloudRenderer.render(this.world, this.client.player, matrices, projectionMatrix, this.ticks, tickDelta, x, y, z);
        }
    }

    @Inject(method = "reload(Lnet/minecraft/resource/ResourceManager;)V", at = @At("RETURN"))
    private void onReload(ResourceManager manager, CallbackInfo ci) {
        if (this.cloudRenderer != null) {
            this.cloudRenderer.reloadTextures(manager);
        }
    }

    @Inject(method = "reload()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        // will be re-allocated on next use
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }

    @Inject(method = "close", at = @At("RETURN"))
    private void onClose(CallbackInfo ci) {
        // will never be re-allocated, as the renderer is shutting down
        if (this.cloudRenderer != null) {
            this.cloudRenderer.destroy();
            this.cloudRenderer = null;
        }
    }
}
