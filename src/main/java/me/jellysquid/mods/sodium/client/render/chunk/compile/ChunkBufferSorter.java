package me.jellysquid.mods.sodium.client.render.chunk.compile;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.format.full.VanillaModelVertexType;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class ChunkBufferSorter {
    public static void sort(ChunkMeshData chunkData, float x, float y, float z) {
        if(chunkData.getVertexData().vertexFormat() != VanillaModelVertexType.VERTEX_FORMAT) {
            return;
        }

        ByteBuffer vertexBuffer = chunkData.getVertexData().vertexBuffer().getDirectBuffer();
        // Vertex stride by Float size
        int vertexStride = chunkData.getVertexData().vertexFormat().getStride();
        int vertexCount = vertexBuffer.capacity() / vertexStride;
        int vertexGroupCount = (vertexCount / 4) * 2; // each quad has 2 index groups (0, 1, 2 and 2, 3, 0)

        float[] distanceArray = new float[vertexGroupCount];
        int[] indicesArray = new int[vertexGroupCount];

        IntBuffer indexBuffer = chunkData.getVertexData().indexBuffer().getDirectBuffer().asIntBuffer();

        int groupPtr = 0;
        FloatBuffer floatBuffer = vertexBuffer.asFloatBuffer();

        while(indexBuffer.hasRemaining()) {
            int v0 = indexBuffer.get();
            int v1 = indexBuffer.get();
            int v2 = indexBuffer.get();

            float groupDistance = getDistanceSqSFP(floatBuffer, x, y, z, vertexStride, v0, v1, v2);
            distanceArray[groupPtr] = groupDistance;
            indicesArray[groupPtr] = groupPtr;
            groupPtr++;
        }

        IntArrays.mergeSort(indicesArray, (a, b) -> Floats.compare(distanceArray[b], distanceArray[a]));

        IntBuffer tmpBuffer = MemoryUtil.memAllocInt(indexBuffer.capacity());
        try {
            // Copy the vertices into the buffer in order
            for(int i = 0; i < vertexGroupCount; i++) {
                int idxBase = indicesArray[i] * 3;
                tmpBuffer.put(indexBuffer.get(idxBase + 0));
                tmpBuffer.put(indexBuffer.get(idxBase + 1));
                tmpBuffer.put(indexBuffer.get(idxBase + 2));
            }
            tmpBuffer.rewind();
            indexBuffer.rewind();
            // Copy this buffer to the original buffer
            MemoryUtil.memCopy(tmpBuffer, indexBuffer);
        } finally {
            MemoryUtil.memFree(tmpBuffer);
        }
    }

    private static float square(float f) {
        return f * f;
    }

    private static float distance(float x1, float x2, float y1, float y2, float z1, float z2) {
        return square(x2 - x1) + square(y2 - y1) + square(z2 - z1);
    }

    private static float getDistanceSqSFP(FloatBuffer buffer, float xCenter, float yCenter, float zCenter, int stride, int v0, int v1, int v2) {
        stride /= 4;

        int vertexBase = v0 * stride;
        float x1 = buffer.get(vertexBase);
        float y1 = buffer.get(vertexBase + 1);
        float z1 = buffer.get(vertexBase + 2);

        vertexBase = v1 * stride;
        float x2 = buffer.get(vertexBase);
        float y2 = buffer.get(vertexBase + 1);
        float z2 = buffer.get(vertexBase + 2);

        vertexBase = v2 * stride;
        float x3 = buffer.get(vertexBase);
        float y3 = buffer.get(vertexBase + 1);
        float z3 = buffer.get(vertexBase + 2);

        float quadX, quadY, quadZ;

        float d12 = distance(x1, x2, y1, y2, z1, z2);
        float d23 = distance(x2, x3, y2, y3, z2, z3);
        float d31 = distance(x3, x1, y3, y1, z3, z1);

        // decide the center of the quad based on the longest side
        if(d12 > d23) {
            if(d12 > d31) {
                quadX = (x1 + x2) / 2;
                quadY = (y1 + y2) / 2;
                quadZ = (z1 + z2) / 2;
            } else {
                quadX = (x3 + x1) / 2;
                quadY = (y3 + y1) / 2;
                quadZ = (z3 + z1) / 2;
            }
        } else {
            if(d23 > d31) {
                quadX = (x2 + x3) / 2;
                quadY = (y2 + y3) / 2;
                quadZ = (z2 + z3) / 2;
            } else {
                quadX = (x3 + x1) / 2;
                quadY = (y3 + y1) / 2;
                quadZ = (z3 + z1) / 2;
            }
        }

        float xDist = quadX - xCenter;
        float yDist = quadY - yCenter;
        float zDist = quadZ - zCenter;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }
}
