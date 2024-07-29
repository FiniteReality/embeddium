package me.jellysquid.mods.sodium.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import com.mojang.math.Vector3f;
import com.mojang.math.Vector4f;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.ColorAttribute;
import net.caffeinemc.mods.sodium.api.vertex.attributes.common.TextureAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import net.minecraft.core.Direction;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SheetedDecalTextureGenerator.class)
public class OverlayVertexConsumerMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private Matrix3f normalInversePose;

    @Shadow
    @Final
    private Matrix4f cameraInversePose;

    @Unique
    private boolean isFullWriter;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.isFullWriter = VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.isFullWriter;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format,
                this.normalInversePose, this.cameraInversePose);

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the overlay UVs element of each vertex to create a perspective-mapped effect.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param inverseNormalMatrix The inverted normal matrix
     * @param inverseTextureMatrix The inverted texture matrix
     */
    @Unique
    private void transform(long ptr, int count, VertexFormatDescription format,
                                  Matrix3f inverseNormalMatrix, Matrix4f inverseTextureMatrix) {
        long stride = format.stride();

        var offsetPosition = format.getElementOffset(CommonVertexAttribute.POSITION);
        var offsetColor = format.getElementOffset(CommonVertexAttribute.COLOR);
        var offsetNormal = format.getElementOffset(CommonVertexAttribute.NORMAL);
        var offsetTexture = format.getElementOffset(CommonVertexAttribute.TEXTURE);

        int color = ColorABGR.pack(1.0f, 1.0f, 1.0f, 1.0f);

        var normal = new Vector3f();
        var position = new Vector4f();

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            position.setX(MemoryUtil.memGetFloat(ptr + offsetPosition + 0));
            position.setY(MemoryUtil.memGetFloat(ptr + offsetPosition + 4));
            position.setZ(MemoryUtil.memGetFloat(ptr + offsetPosition + 8));
            position.setW(1.0f);

            int packedNormal = MemoryUtil.memGetInt(ptr + offsetNormal);
            normal.setX(NormI8.unpackX(packedNormal));
            normal.setY(NormI8.unpackY(packedNormal));
            normal.setZ(NormI8.unpackZ(packedNormal));

            normal.transform(inverseNormalMatrix);
            Direction direction = Direction.getNearest(normal.x(), normal.y(), normal.z());

            position.transform(inverseTextureMatrix);
            position.transform(Vector3f.YP.rotation(3.1415927F));
            position.transform(Vector3f.XP.rotation(-1.5707964F));
            position.transform(direction.getRotation());

            float textureU = -position.x();
            float textureV = -position.y();

            ColorAttribute.set(ptr + offsetColor, color);
            TextureAttribute.put(ptr + offsetTexture, textureU, textureV);

            ptr += stride;
        }
    }
}
