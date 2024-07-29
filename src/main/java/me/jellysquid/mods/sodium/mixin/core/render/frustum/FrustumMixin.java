package me.jellysquid.mods.sodium.mixin.core.render.frustum;

import com.mojang.math.Matrix4f;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    private FrustumIntersection intersectionMatrix;

    @Inject(method = "<init>(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;)V", at = @At("RETURN"))
    private void initFrustum(Matrix4f pProjection, Matrix4f pFrustum, CallbackInfo ci) {
        this.intersectionMatrix = new FrustumIntersection(JomlHelper.copy(pFrustum).mul(JomlHelper.copy(pProjection)), false);
    }

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(new SimpleFrustum(this.intersectionMatrix), new Vector3d(this.camX, this.camY, this.camZ));
    }
}
