package me.jellysquid.mods.sodium.client.render.chunk.compile;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.VanillaLikeChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.full.VanillaModelVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.sfp.ModelVertexType;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class ChunkBufferSorter {
    enum PositionType {
        COMPACT_POSITION,
        FLOAT_POSITION,
        UNKNOWN_POSITION
    }

    private static final ConcurrentHashMap<GlVertexFormat, PositionType> POSITIONS_BY_FORMAT = new ConcurrentHashMap<>();

    public static void sort(ChunkMeshData chunkData, float x, float y, float z) {
        PositionType type = POSITIONS_BY_FORMAT.computeIfAbsent(chunkData.getVertexData().vertexFormat(), format -> {
            try {
                format.getAttribute(VanillaLikeChunkMeshAttribute.POSITION);
                return PositionType.FLOAT_POSITION;
            } catch(NullPointerException e) {}
            try {
                format.getAttribute(ChunkMeshAttribute.POSITION_ID);
                return PositionType.COMPACT_POSITION;
            } catch(NullPointerException e) {}
            SodiumClientMod.logger().warn("Don't know how to sort {}", format.toString());
            return PositionType.UNKNOWN_POSITION;
        });

        if(type == PositionType.UNKNOWN_POSITION)
            return;

        ByteBuffer vertexBuffer = chunkData.getVertexData().vertexBuffer().getDirectBuffer();
        // Vertex stride by Float size
        int vertexStride = chunkData.getVertexData().vertexFormat().getStride();

        IntBuffer indexBuffer = chunkData.getVertexData().indexBuffer().getDirectBuffer().asIntBuffer();
        int vertexGroupCount = indexBuffer.capacity() / 3;

        float[] distanceArray = new float[vertexGroupCount];
        int[] indicesArray = new int[vertexGroupCount];


        int groupPtr = 0;

        while(indexBuffer.hasRemaining()) {
            int v0 = indexBuffer.get();
            int v1 = indexBuffer.get();
            int v2 = indexBuffer.get();

            float groupDistance = getDistanceSqSFP(vertexBuffer, type, x, y, z, vertexStride, v0, v1, v2);
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

    private static float getX(ByteBuffer buffer, int vertexBaseBytes, PositionType type) {
        if(type == PositionType.FLOAT_POSITION)
            return buffer.getFloat(vertexBaseBytes);
        return ModelVertexType.decodePosition(buffer.getShort(vertexBaseBytes));
    }

    private static float getY(ByteBuffer buffer, int vertexBaseBytes, PositionType type) {
        if(type == PositionType.FLOAT_POSITION)
            return buffer.getFloat(vertexBaseBytes + 4);
        return ModelVertexType.decodePosition(buffer.getShort(vertexBaseBytes + 2));
    }

    private static float getZ(ByteBuffer buffer, int vertexBaseBytes, PositionType type) {
        if(type == PositionType.FLOAT_POSITION)
            return buffer.getFloat(vertexBaseBytes + 8);
        return ModelVertexType.decodePosition(buffer.getShort(vertexBaseBytes + 4));
    }

    private static float getDistanceSqSFP(ByteBuffer buffer, PositionType type, float xCenter, float yCenter, float zCenter, int stride, int v0, int v1, int v2) {
        int vertexBase = v0 * stride;
        float x1 = getX(buffer, vertexBase, type);
        float y1 = getY(buffer, vertexBase, type);
        float z1 = getZ(buffer, vertexBase, type);

        vertexBase = v1 * stride;
        float x2 = getX(buffer, vertexBase, type);
        float y2 = getY(buffer, vertexBase, type);
        float z2 = getZ(buffer, vertexBase, type);

        vertexBase = v2 * stride;
        float x3 = getX(buffer, vertexBase, type);
        float y3 = getY(buffer, vertexBase, type);
        float z3 = getZ(buffer, vertexBase, type);

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
