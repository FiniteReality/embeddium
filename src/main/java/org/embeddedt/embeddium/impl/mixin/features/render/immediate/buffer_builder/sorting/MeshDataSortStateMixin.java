package org.embeddedt.embeddium.impl.mixin.features.render.immediate.buffer_builder.sorting;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;

@Mixin(MeshData.SortState.class)
public class MeshDataSortStateMixin {
    @Shadow
    @Final
    private Vector3f[] centroids;

    @Shadow
    @Final
    private VertexFormat.IndexType indexType;

    /**
     * @author JellySquid
     * @reason Use direct memory access, avoid indirection
     */
    @Overwrite
    public ByteBufferBuilder.Result buildSortedIndexBuffer(ByteBufferBuilder buffer, VertexSorting sorting) {
        int[] indices = sorting.sort(this.centroids);
        this.writePrimitiveIndices(buffer, indices);
        return buffer.build();
    }

    @Unique
    private static final int[] VERTEX_ORDER = new int[] { 0, 1, 2, 2, 3, 0 };

    @Unique
    private void writePrimitiveIndices(ByteBufferBuilder builder, int[] indices) {
        long ptr = builder.reserve(indices.length * 6 * this.indexType.bytes);

        switch (indexType.bytes) {
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
        }
    }
}
