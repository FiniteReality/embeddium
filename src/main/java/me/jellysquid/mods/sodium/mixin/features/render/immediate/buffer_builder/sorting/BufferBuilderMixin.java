
package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder.sorting;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.math.Vector3f;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.nio.ByteBuffer;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin {
    @Shadow
    private ByteBuffer buffer;

    @Shadow
    private int nextElementByte;

    @Shadow
    @Nullable
    private Vector3f[] sortingPoints;

    @Shadow
    private int vertices;

    @Shadow
    private VertexFormat format;

    @Shadow
    private int renderedBufferPointer;

    /**
     * @author JellySquid
     * @reason Avoid slow memory accesses
     */
    @Overwrite
    private Vector3f[] makeQuadSortingPoints() {
        int vertexStride = this.format.getVertexSize();
        int primitiveCount = this.vertices / 4;

        Vector3f[] centers = new Vector3f[primitiveCount];

        for (int index = 0; index < primitiveCount; ++index) {
            long v1 = MemoryUtil.memAddress(this.buffer, this.renderedBufferPointer + (((index * 4) + 0) * vertexStride));
            long v2 = MemoryUtil.memAddress(this.buffer, this.renderedBufferPointer + (((index * 4) + 2) * vertexStride));

            float x1 = MemoryUtil.memGetFloat(v1 + 0);
            float y1 = MemoryUtil.memGetFloat(v1 + 4);
            float z1 = MemoryUtil.memGetFloat(v1 + 8);

            float x2 = MemoryUtil.memGetFloat(v2 + 0);
            float y2 = MemoryUtil.memGetFloat(v2 + 4);
            float z2 = MemoryUtil.memGetFloat(v2 + 8);

            centers[index] = new Vector3f((x1 + x2) * 0.5F, (y1 + y2) * 0.5F, (z1 + z2) * 0.5F);
        }

        return centers;
    }

    /**
     * @author JellySquid
     * @reason Use direct memory access, avoid indirection
     */
    @Inject(method = "putSortedQuadIndices", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;intConsumer(ILcom/mojang/blaze3d/vertex/VertexFormat$IndexType;)Lit/unimi/dsi/fastutil/ints/IntConsumer;"), cancellable = true)
    private void putSortedQuadIndices(VertexFormat.IndexType indexType, CallbackInfo ci, @Local(ordinal = 0) int[] indices) {
        ci.cancel();
        this.writePrimitiveIndices(indexType, indices);
    }

    @Unique
    private static final int[] VERTEX_ORDER = new int[] { 0, 1, 2, 2, 3, 0 };

    @Unique
    private void writePrimitiveIndices(VertexFormat.IndexType indexType, int[] indices) {
        long ptr = MemoryUtil.memAddress(this.buffer, this.nextElementByte);

        switch (indexType.bytes) {
            case 1 -> { // BYTE
                for (int index : indices) {
                    int start = index * 4;

                    for (int offset : VERTEX_ORDER) {
                        MemoryUtil.memPutByte(ptr, (byte) (start + offset));
                        ptr += Byte.BYTES;
                    }
                }
            }
            case 2 -> { // SHORT
                for (int index : indices) {
                    int start = index * 4;

                    for (int offset : VERTEX_ORDER) {
                        MemoryUtil.memPutShort(ptr, (short) (start + offset));
                        ptr += Short.BYTES;
                    }
                }
            }
            case 4 -> { // INT
                for (int index : indices) {
                    int start = index * 4;

                    for (int offset : VERTEX_ORDER) {
                        MemoryUtil.memPutInt(ptr, (start + offset));
                        ptr += Integer.BYTES;
                    }
                }
            }
            default -> throw new IllegalStateException("Unexpected index size: " + indexType.bytes);
        }
    }
}