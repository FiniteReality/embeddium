package me.jellysquid.mods.sodium.mixin.core.render.frustum;

import com.mojang.math.Matrix4f;
import com.mojang.math.Vector4f;
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
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(Frustum.class)
public class FrustumMixin implements ViewportProvider {
    @Shadow
    private double camX;

    @Shadow
    private double camY;

    @Shadow
    private double camZ;

    private Vector4f embeddium$viewVector;

    private FrustumIntersection intersectionMatrix;

    @Inject(method = "<init>(Lcom/mojang/math/Matrix4f;Lcom/mojang/math/Matrix4f;)V", at = @At("RETURN"))
    private void initFrustum(Matrix4f pProjection, Matrix4f pFrustum, CallbackInfo ci) {
        this.intersectionMatrix = new FrustumIntersection(JomlHelper.copy(pFrustum).mul(JomlHelper.copy(pProjection)), false);
    }

    /**
     * @author embeddedt
     * @reason backported magic from modern MC to fix frustum giving false results at some angles
     */
    @Inject(method = "prepare", at = @At("RETURN"))
    private void offsetToFullyIncludeCameraCube(double camX, double camY, double camZ, CallbackInfo ci) {
        if (this.embeddium$viewVector == null || Float.isNaN(this.embeddium$viewVector.x()) || Float.isNaN(this.embeddium$viewVector.y())
                || Float.isNaN(this.embeddium$viewVector.z()) || Float.isNaN(this.embeddium$viewVector.w())) {
            // 1.16 quirk - do nothing
            return;
        }
        int offset = 8;
        double fX = Math.floor(this.camX / (double)offset) * (double)offset;
        double fY = Math.floor(this.camY / (double)offset) * (double)offset;
        double fZ = Math.floor(this.camZ / (double)offset) * (double)offset;
        double cX = Math.ceil(this.camX / (double)offset) * (double)offset;
        double cY = Math.ceil(this.camY / (double)offset) * (double)offset;
        double cZ = Math.ceil(this.camZ / (double)offset) * (double)offset;

        while (this.intersectionMatrix.intersectAab(
                (float)(fX - this.camX), (float)(fY - this.camY), (float)(fZ - this.camZ),
                (float)(cX - this.camX), (float)(cY - this.camY), (float)(cZ - this.camZ))
                != FrustumIntersection.INSIDE) {
            this.camX = this.camX - (double)(this.embeddium$viewVector.x() * 4.0F);
            this.camY = this.camY - (double)(this.embeddium$viewVector.y() * 4.0F);
            this.camZ = this.camZ - (double)(this.embeddium$viewVector.z() * 4.0F);
        }
    }

    @Inject(method = "calculateFrustum", at = @At("RETURN"), locals = LocalCapture.CAPTURE_FAILHARD)
    private void calculateViewVector(Matrix4f projection, Matrix4f frustrumMatrix, CallbackInfo ci, Matrix4f frustumPlaneMatrix) {
        this.embeddium$viewVector = new Vector4f(0, 0, 1, 0);
        this.embeddium$viewVector.transform(frustumPlaneMatrix);
    }

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(new SimpleFrustum(this.intersectionMatrix), new Vector3d(this.camX, this.camY, this.camZ));
    }
}
