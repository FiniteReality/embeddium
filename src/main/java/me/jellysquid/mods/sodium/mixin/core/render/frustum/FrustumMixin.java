package me.jellysquid.mods.sodium.mixin.core.render.frustum;

import com.mojang.math.Matrix4f;
import me.jellysquid.mods.sodium.client.render.viewport.frustum.SimpleFrustum;
import net.minecraft.client.renderer.culling.Frustum;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.render.viewport.ViewportProvider;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.extensions.IForgeTileEntity;
import org.embeddedt.embeddium.api.math.JomlHelper;
import org.joml.FrustumIntersection;
import org.joml.Vector3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Frustum.class)
public abstract class FrustumMixin implements ViewportProvider {
    @Shadow public abstract boolean cubeInFrustum(double minX, double minY, double minZ, double maxX, double maxY, double maxZ);

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

    /**
     * @author XFactHD (ported by embeddedt)
     * @reason Avoid passing infinite extents box into optimized frustum code.
     * This is a port of <a href="https://github.com/MinecraftForge/MinecraftForge/pull/9407">PR #9407</a>
     */
    @Overwrite
    public boolean isVisible(AABB box) {
        if(box.equals(IForgeTileEntity.INFINITE_EXTENT_AABB))
            return true;
        return this.cubeInFrustum(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    /**
     * @author embeddedt
     * @reason Use JOML frustum logic instead of dot products
     */
    @Overwrite
    private boolean cubeInFrustum(float minX, float minY, float minZ, float maxX, float maxY, float maxZ) {
        return this.intersectionMatrix.testAab(minX, minY, minZ, maxX, maxY, maxZ);
    }

    @Override
    public Viewport sodium$createViewport() {
        return new Viewport(new SimpleFrustum(this.intersectionMatrix), new Vector3d(this.camX, this.camY, this.camZ));
    }
}
