package me.jellysquid.mods.sodium.mixin.features.render.particle;

import net.caffeinemc.mods.sodium.api.vertex.format.common.ParticleVertex;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.minecraft.client.particle.BillboardParticle;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BillboardParticle.class)
public abstract class BillboardParticleMixin extends Particle {
    @Shadow
    public abstract float getSize(float tickDelta);

    @Shadow
    protected abstract float getMinU();

    @Shadow
    protected abstract float getMaxU();

    @Shadow
    protected abstract float getMinV();

    @Shadow
    protected abstract float getMaxV();

    protected BillboardParticleMixin(ClientWorld world, double x, double y, double z) {
        super(world, x, y, z);
    }

    /**
     * @reason Optimize function
     * @author JellySquid
     */
    @Inject(method = "buildGeometry", at = @At("HEAD"), cancellable = true)
    private void buildGeometryFast(VertexConsumer vertexConsumer, Camera camera, float tickDelta, CallbackInfo ci) {
        var writer = VertexBufferWriter.tryOf(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        Vec3d vec3d = camera.getPos();

        float x = (float) (MathHelper.lerp(tickDelta, this.prevPosX, this.x) - vec3d.getX());
        float y = (float) (MathHelper.lerp(tickDelta, this.prevPosY, this.y) - vec3d.getY());
        float z = (float) (MathHelper.lerp(tickDelta, this.prevPosZ, this.z) - vec3d.getZ());

        Quaternionf quaternion;

        if (this.angle == 0.0F) {
            quaternion = camera.getRotation();
        } else {
            float angle = MathHelper.lerp(tickDelta, this.prevAngle, this.angle);

            quaternion = new Quaternionf(camera.getRotation());
            quaternion.rotateZ(angle);
        }

        float size = this.getSize(tickDelta);
        int light = this.getBrightness(tickDelta);

        float minU = this.getMinU();
        float maxU = this.getMaxU();
        float minV = this.getMinV();
        float maxV = this.getMaxV();

        int color = ColorABGR.pack(this.red , this.green, this.blue, this.alpha);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ParticleVertex.STRIDE);
            long ptr = buffer;

            writeVertex(ptr, quaternion,-1.0F, -1.0F, x, y, z, maxU, maxV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writeVertex(ptr, quaternion,-1.0F, 1.0F, x, y, z, maxU, minV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writeVertex(ptr, quaternion,1.0F, 1.0F, x, y, z, minU, minV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writeVertex(ptr, quaternion,1.0F, -1.0F, x, y, z, minU, maxV, color, light, size);
            ptr += ParticleVertex.STRIDE;

            writer.push(stack, buffer, 4, ParticleVertex.FORMAT);
        }

    }

    @Unique
    @SuppressWarnings("UnnecessaryLocalVariable")
    private static void writeVertex(long buffer,
                                    Quaternionf rotation,
                                    float posX, float posY,
                                    float originX, float originY, float originZ,
                                    float u, float v, int color, int light, float size) {
        // Quaternion q0 = new Quaternion(rotation);
        float q0x = rotation.x();
        float q0y = rotation.y();
        float q0z = rotation.z();
        float q0w = rotation.w();

        // q0.hamiltonProduct(x, y, 0.0f, 0.0f)
        float q1x = (q0w * posX) - (q0z * posY);
        float q1y = (q0w * posY) + (q0z * posX);
        float q1w = (q0x * posY) - (q0y * posX);
        float q1z = -(q0x * posX) - (q0y * posY);

        // Quaternion q2 = new Quaternion(rotation);
        // q2.conjugate()
        float q2x = -q0x;
        float q2y = -q0y;
        float q2z = -q0z;
        float q2w = q0w;

        // q2.hamiltonProduct(q1)
        float q3x = q1z * q2x + q1x * q2w + q1y * q2z - q1w * q2y;
        float q3y = q1z * q2y - q1x * q2z + q1y * q2w + q1w * q2x;
        float q3z = q1z * q2z + q1x * q2y - q1y * q2x + q1w * q2w;

        // Vector3f f = new Vector3f(q2.getX(), q2.getY(), q2.getZ())
        // f.multiply(size)
        // f.add(pos)
        float fx = (q3x * size) + originX;
        float fy = (q3y * size) + originY;
        float fz = (q3z * size) + originZ;

        ParticleVertex.put(buffer, fx, fy, fz, u, v, color, light);
    }
}
