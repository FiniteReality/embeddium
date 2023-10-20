package me.jellysquid.mods.sodium.client.render.chunk.compile;

import com.google.common.primitives.Floats;
import it.unimi.dsi.fastutil.ints.IntArrays;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.CompactChunkVertex;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl.VanillaLikeChunkVertex;
import org.lwjgl.system.MemoryStack;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.ShortBuffer;
import java.util.BitSet;

public class ChunkBufferSorter {
    public record SortBuffer(ByteBuffer vertexBuffer, ChunkVertexType format, VertexRange[] ranges) {
        private static ByteBuffer cloneBuf(ByteBuffer b) {
            ByteBuffer clone = ByteBuffer.allocate(b.capacity()).order(b.order());
            b.rewind();//copy from the beginning
            clone.put(b);
            b.rewind();
            clone.flip();
            return clone;
        }

        public SortBuffer duplicate() {
            if(vertexBuffer.isDirect())
                throw new IllegalStateException("Cannot duplicate direct SortBuffer");
            return new SortBuffer(cloneBuf(vertexBuffer), format, ranges);
        }

    }

    public static void sort(SortBuffer chunkData, float x, float y, float z) {
        boolean isCompact = !(chunkData.format() instanceof VanillaLikeChunkVertex);

        ByteBuffer buffer = chunkData.vertexBuffer();
        int bufferLen = buffer.capacity();

        // Quad stride by Float size
        int quadStride = chunkData.format().getVertexFormat().getStride();

        int quadStart = ((Buffer)buffer).position();
        int quadCount = bufferLen/quadStride/4;

        float[] distanceArray = new float[quadCount];
        int[] indicesArray = new int[quadCount];

        if(isCompact) {
            ShortBuffer shortBuffer = buffer.asShortBuffer();
            int vertexSizeShort = quadStride / 2;
            for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
                distanceArray[quadIdx] = getDistanceSqHFP(shortBuffer, x, y, z, vertexSizeShort, quadStart + (quadIdx * quadStride * 2));
                indicesArray[quadIdx] = quadIdx;
            }
        } else {
            FloatBuffer floatBuffer = buffer.asFloatBuffer();
            int vertexSizeInteger = quadStride / 4;
            for (int quadIdx = 0; quadIdx < quadCount; ++quadIdx) {
                distanceArray[quadIdx] = getDistanceSqSFP(floatBuffer, x, y, z, vertexSizeInteger, quadStart + (quadIdx * quadStride));
                indicesArray[quadIdx] = quadIdx;
            }
        }

        IntArrays.mergeSort(indicesArray, (a, b) -> Floats.compare(distanceArray[b], distanceArray[a]));

        rearrangeQuads(buffer, indicesArray, quadStride, quadStart);
    }

    private static void rearrangeQuads(ByteBuffer quadBuffer, int[] indicesArray, int quadStride, int quadStart) {
        FloatBuffer floatBuffer = quadBuffer.asFloatBuffer();
        BitSet bits = new BitSet();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer tmp = stack.mallocFloat(quadStride);

            for (int l = bits.nextClearBit(0); l < indicesArray.length; l = bits.nextClearBit(l + 1)) {
                int m = indicesArray[l];

                if (m != l) {
                    sliceQuad(floatBuffer, m, quadStride, quadStart);
                    ((Buffer)tmp).clear();
                    tmp.put(floatBuffer);

                    int n = m;

                    for (int o = indicesArray[m]; n != l; o = indicesArray[o]) {
                        sliceQuad(floatBuffer, o, quadStride, quadStart);
                        FloatBuffer floatBuffer3 = floatBuffer.slice();

                        sliceQuad(floatBuffer, n, quadStride, quadStart);
                        floatBuffer.put(floatBuffer3);

                        bits.set(n);
                        n = o;
                    }

                    sliceQuad(floatBuffer, l, quadStride, quadStart);
                    ((Buffer)tmp).flip();

                    floatBuffer.put(tmp);
                }

                bits.set(l);
            }
        }
    }

    private static void sliceQuad(FloatBuffer floatBuffer, int quadIdx, int quadStride, int quadStart) {
        int base = quadStart + (quadIdx * quadStride);

        ((Buffer)floatBuffer).limit(base + quadStride);
        ((Buffer)floatBuffer).position(base);
    }

    private static float getDistanceSqSFP(FloatBuffer buffer, float xCenter, float yCenter, float zCenter, int stride, int start) {
        int vertexBase = start;
        float x1 = buffer.get(vertexBase);
        float y1 = buffer.get(vertexBase + 1);
        float z1 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        float x2 = buffer.get(vertexBase);
        float y2 = buffer.get(vertexBase + 1);
        float z2 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        float x3 = buffer.get(vertexBase);
        float y3 = buffer.get(vertexBase + 1);
        float z3 = buffer.get(vertexBase + 2);

        vertexBase += stride;
        float x4 = buffer.get(vertexBase);
        float y4 = buffer.get(vertexBase + 1);
        float z4 = buffer.get(vertexBase + 2);

        float xDist = ((x1 + x2 + x3 + x4) * 0.25F) - xCenter;
        float yDist = ((y1 + y2 + y3 + y4) * 0.25F) - yCenter;
        float zDist = ((z1 + z2 + z3 + z4) * 0.25F) - zCenter;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }

    private static float getDistanceSqHFP(ShortBuffer buffer, float xCenter, float yCenter, float zCenter, int stride, int start) {
        int vertexBase = start;
        float x1 = CompactChunkVertex.decodePosition(buffer.get(vertexBase));
        float y1 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 1));
        float z1 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 2));

        vertexBase += stride;
        float x2 = CompactChunkVertex.decodePosition(buffer.get(vertexBase));
        float y2 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 1));
        float z2 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 2));

        vertexBase += stride;
        float x3 = CompactChunkVertex.decodePosition(buffer.get(vertexBase));
        float y3 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 1));
        float z3 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 2));

        vertexBase += stride;
        float x4 = CompactChunkVertex.decodePosition(buffer.get(vertexBase));
        float y4 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 1));
        float z4 = CompactChunkVertex.decodePosition(buffer.get(vertexBase + 2));

        float xDist = ((x1 + x2 + x3 + x4) * 0.25F) - xCenter;
        float yDist = ((y1 + y2 + y3 + y4) * 0.25F) - yCenter;
        float zDist = ((z1 + z2 + z3 + z4) * 0.25F) - zCenter;

        return (xDist * xDist) + (yDist * yDist) + (zDist * zDist);
    }
}
