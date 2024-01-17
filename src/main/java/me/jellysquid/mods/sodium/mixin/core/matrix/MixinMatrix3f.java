package me.jellysquid.mods.sodium.mixin.core.matrix;

import com.mojang.math.Matrix3f;
import com.mojang.math.Quaternion;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.math.Matrix3fExtended;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Matrix3f.class)
public class MixinMatrix3f implements Matrix3fExtended {
    @Shadow
    protected float m00;

    @Shadow
    protected float m10;

    @Shadow
    protected float m20;

    @Shadow
    protected float m01;

    @Shadow
    protected float m11;

    @Shadow
    protected float m21;

    @Shadow
    protected float m02;

    @Shadow
    protected float m12;

    @Shadow
    protected float m22;

    @Override
    public float transformVecX(float x, float y, float z) {
        return this.m00 * x + this.m01 * y + this.m02 * z;
    }

    @Override
    public float transformVecY(float x, float y, float z) {
        return this.m10 * x + this.m11 * y + this.m12 * z;
    }

    @Override
    public float transformVecZ(float x, float y, float z) {
        return this.m20 * x + this.m21 * y + this.m22 * z;
    }

    @Override
    public void rotate(Quaternion quaternion) {
        boolean x = quaternion.i() != 0.0F;
        boolean y = quaternion.j() != 0.0F;
        boolean z = quaternion.k() != 0.0F;

        // Try to determine if this is a simple rotation on one axis component only
        if (x) {
            if (!y && !z) {
                this.rotateX(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (y) {
            if (!z) {
                this.rotateY(quaternion);
            } else {
                this.rotateXYZ(quaternion);
            }
        } else if (z) {
            this.rotateZ(quaternion);
        }
    }

    @Override
    public int computeNormal(Direction dir) {
        Vec3i faceNorm = dir.getNormal();

        float x = faceNorm.getX();
        float y = faceNorm.getY();
        float z = faceNorm.getZ();

        float x2 = this.m00 * x + this.m01 * y + this.m02 * z;
        float y2 = this.m10 * x + this.m11 * y + this.m12 * z;
        float z2 = this.m20 * x + this.m21 * y + this.m22 * z;

        return Norm3b.pack(x2, y2, z2);
    }

    private void rotateX(Quaternion quaternion) {
        float x = quaternion.i();
        float w = quaternion.r();

        float xx = 2.0F * x * x;

        float ta11 = 1.0F - xx;
        float ta22 = 1.0F - xx;

        float xw = x * w;
        float ta21 = 2.0F * xw;
        float ta12 = 2.0F * -xw;

        float a01 = this.m01 * ta11 + this.m02 * ta21;
        float a02 = this.m01 * ta12 + this.m02 * ta22;
        float a11 = this.m11 * ta11 + this.m12 * ta21;
        float a12 = this.m11 * ta12 + this.m12 * ta22;
        float a21 = this.m21 * ta11 + this.m22 * ta21;
        float a22 = this.m21 * ta12 + this.m22 * ta22;

        this.m01 = a01;
        this.m02 = a02;
        this.m11 = a11;
        this.m12 = a12;
        this.m21 = a21;
        this.m22 = a22;
    }

    private void rotateY(Quaternion quaternion) {
        float y = quaternion.j();
        float w = quaternion.r();

        float yy = 2.0F * y * y;

        float ta00 = 1.0F - yy;
        float ta22 = 1.0F - yy;

        float yw = y * w;

        float ta20 = 2.0F * (-yw);
        float ta02 = 2.0F * (+yw);

        float a00 = this.m00 * ta00 + this.m02 * ta20;
        float a02 = this.m00 * ta02 + this.m02 * ta22;
        float a10 = this.m10 * ta00 + this.m12 * ta20;
        float a12 = this.m10 * ta02 + this.m12 * ta22;
        float a20 = this.m20 * ta00 + this.m22 * ta20;
        float a22 = this.m20 * ta02 + this.m22 * ta22;

        this.m00 = a00;
        this.m02 = a02;
        this.m10 = a10;
        this.m12 = a12;
        this.m20 = a20;
        this.m22 = a22;
    }

    private void rotateZ(Quaternion quaternion) {
        float z = quaternion.k();
        float w = quaternion.r();

        float zz = 2.0F * z * z;

        float ta00 = 1.0F - zz;
        float ta11 = 1.0F - zz;

        float zw = z * w;

        float ta10 = 2.0F * (0.0F + zw);
        float ta01 = 2.0F * (0.0F - zw);

        float a00 = this.m00 * ta00 + this.m01 * ta10;
        float a01 = this.m00 * ta01 + this.m01 * ta11;
        float a10 = this.m10 * ta00 + this.m11 * ta10;
        float a11 = this.m10 * ta01 + this.m11 * ta11;
        float a20 = this.m20 * ta00 + this.m21 * ta10;
        float a21 = this.m20 * ta01 + this.m21 * ta11;

        this.m00 = a00;
        this.m01 = a01;
        this.m10 = a10;
        this.m11 = a11;
        this.m20 = a20;
        this.m21 = a21;
    }

    private void rotateXYZ(Quaternion quaternion) {
        float x = quaternion.i();
        float y = quaternion.j();
        float z = quaternion.k();
        float w = quaternion.r();

        float xx = 2.0F * x * x;
        float yy = 2.0F * y * y;
        float zz = 2.0F * z * z;

        float ta00 = 1.0F - yy - zz;
        float ta11 = 1.0F - zz - xx;
        float ta22 = 1.0F - xx - yy;

        float xy = x * y;
        float yz = y * z;
        float zx = z * x;
        float xw = x * w;
        float yw = y * w;
        float zw = z * w;

        float ta10 = 2.0F * (xy + zw);
        float ta01 = 2.0F * (xy - zw);
        float ta20 = 2.0F * (zx - yw);
        float ta02 = 2.0F * (zx + yw);
        float ta21 = 2.0F * (yz + xw);
        float ta12 = 2.0F * (yz - xw);

        float a00 = this.m00 * ta00 + this.m01 * ta10 + this.m02 * ta20;
        float a01 = this.m00 * ta01 + this.m01 * ta11 + this.m02 * ta21;
        float a02 = this.m00 * ta02 + this.m01 * ta12 + this.m02 * ta22;
        float a10 = this.m10 * ta00 + this.m11 * ta10 + this.m12 * ta20;
        float a11 = this.m10 * ta01 + this.m11 * ta11 + this.m12 * ta21;
        float a12 = this.m10 * ta02 + this.m11 * ta12 + this.m12 * ta22;
        float a20 = this.m20 * ta00 + this.m21 * ta10 + this.m22 * ta20;
        float a21 = this.m20 * ta01 + this.m21 * ta11 + this.m22 * ta21;
        float a22 = this.m20 * ta02 + this.m21 * ta12 + this.m22 * ta22;

        this.m00 = a00;
        this.m01 = a01;
        this.m02 = a02;
        this.m10 = a10;
        this.m11 = a11;
        this.m12 = a12;
        this.m20 = a20;
        this.m21 = a21;
        this.m22 = a22;
    }
}
