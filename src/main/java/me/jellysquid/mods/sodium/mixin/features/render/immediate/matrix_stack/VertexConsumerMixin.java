package me.jellysquid.mods.sodium.mixin.features.render.immediate.matrix_stack;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import org.embeddedt.embeddium.api.math.Matrix3fExtended;
import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin {
    @Shadow
    VertexConsumer normal(float x, float y, float z);

    @Shadow
    VertexConsumer vertex(double x, double y, double z);

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer vertex(Matrix4f matrix, float x, float y, float z) {
        float xt = ((Matrix4fExtended)(Object)matrix).transformVecX(x, y, z);
        float yt = ((Matrix4fExtended)(Object)matrix).transformVecY(x, y, z);
        float zt = ((Matrix4fExtended)(Object)matrix).transformVecZ(x, y, z);

        return this.vertex(xt, yt, zt);
    }

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Overwrite
    default VertexConsumer normal(Matrix3f matrix, float x, float y, float z) {
        var matrixExt = Matrix3fExtended.get(matrix);
        float xt = matrixExt.transformVecX(x, y, z);
        float yt = matrixExt.transformVecY(x, y, z);
        float zt = matrixExt.transformVecZ(x, y, z);

        return this.normal(xt, yt, zt);
    }
}
