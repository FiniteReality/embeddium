package me.jellysquid.mods.sodium.client.render.chunk.compile;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.Int2IntFunction;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.TranslucentQuadAnalyzer;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.*;
import java.util.BitSet;

public class ChunkBufferSorter {
    private static final int ELEMENTS_PER_PRIMITIVE = 6;
    private static final int VERTICES_PER_PRIMITIVE = 4;

    public static int getIndexBufferSize(int numPrimitives) {
        return numPrimitives * ELEMENTS_PER_PRIMITIVE * 4;
    }

    private static NativeBuffer generateIndexBuffer(NativeBuffer indexBuffer, int[] primitiveMapping) {
        int bufferSize = getIndexBufferSize(primitiveMapping.length);
        if(indexBuffer.getLength() != bufferSize) {
            throw new IllegalStateException("Given index buffer has length " + indexBuffer.getLength() + " but we expected " + bufferSize);
        }
        long ptr = MemoryUtil.memAddress(indexBuffer.getDirectBuffer());

        for (int primitiveIndex = 0; primitiveIndex < primitiveMapping.length; primitiveIndex++) {
            int indexOffset = primitiveIndex * ELEMENTS_PER_PRIMITIVE;

            // Map to the desired primitive
            int vertexOffset = primitiveMapping[primitiveIndex] * VERTICES_PER_PRIMITIVE;

            MemoryUtil.memPutInt(ptr + (indexOffset + 0) * 4, vertexOffset + 0);
            MemoryUtil.memPutInt(ptr + (indexOffset + 1) * 4, vertexOffset + 1);
            MemoryUtil.memPutInt(ptr + (indexOffset + 2) * 4, vertexOffset + 2);

            MemoryUtil.memPutInt(ptr + (indexOffset + 3) * 4, vertexOffset + 2);
            MemoryUtil.memPutInt(ptr + (indexOffset + 4) * 4, vertexOffset + 3);
            MemoryUtil.memPutInt(ptr + (indexOffset + 5) * 4, vertexOffset + 0);
        }

        return indexBuffer;
    }

    public static NativeBuffer sort(NativeBuffer indexBuffer, @Nullable TranslucentQuadAnalyzer.SortState chunkData, float x, float y, float z) {
        if (chunkData == null) {
            return indexBuffer;
        }

        float[] centers = chunkData.centers();
        int quadCount = centers.length / 3;
        int[] indicesArray = new int[quadCount];
        float[] distanceArray = new float[quadCount];
        for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
            indicesArray[quadIdx] = quadIdx;
            int centerIdx = quadIdx * 3;
            float qX = centers[centerIdx + 0] - x;
            float qY = centers[centerIdx + 1] - y;
            float qZ = centers[centerIdx + 2] - z;
            distanceArray[quadIdx] = qX * qX + qY * qY + qZ * qZ;
        }

        IntArrays.mergeSort(indicesArray, (a, b) -> Floats.compare(distanceArray[b], distanceArray[a]));

        return generateIndexBuffer(indexBuffer, indicesArray);
    }
}
