package me.jellysquid.mods.sodium.client.compat.ccl;

import me.jellysquid.mods.sodium.client.model.IndexBufferBuilder;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import javax.annotation.Nonnull;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

/**
 * A {@link VertexConsumer} implementation which amortizes allocations
 * and pipes vertices into Sodium's meshing system.
 *
 * @author KitsuneAlex, embeddedt
 */
@OnlyIn(Dist.CLIENT)
public final class SinkingVertexBuilder implements VertexConsumer {
    private static final int VERTEX_SIZE_BYTES = 32;
    private static final int INITIAL_CAPACITY = 16384; // Seems to generally be enough for your average subchunk
    private static final ByteBuffer EMPTY_BUFFER = ByteBuffer.allocate(0);
    private ByteBuffer buffer = EMPTY_BUFFER;

    private final int[] sideCount = new int[ModelQuadFacing.VALUES.length];
    private int currentVertex;

    private float x;
    private float y;
    private float z;
    private float nx;
    private float ny;
    private float nz;
    private float u;
    private float v;
    private int color;
    private int light;

    private int fixedColor;
    private boolean hasFixedColor = false;

    private static ByteBuffer reallocDirect(ByteBuffer old, int capacity) {
        ByteBuffer newBuf = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        int oldPos = old.position();
        old.rewind();
        newBuf.put(old);
        newBuf.position(Math.min(capacity, oldPos));
        old.position(oldPos);
        return newBuf;
    }

    @Nonnull
    @Override
    public VertexConsumer vertex(double x, double y, double z) {
        this.x = (float) x;
        this.y = (float) y;
        this.z = (float) z;
        return this;
    }

    @Nonnull
    @Override
    public VertexConsumer color(int r, int g, int b, int a) {
        color = ((a & 255) << 24) | ((b & 255) << 16) | ((g & 255) << 8) | (r & 255);
        // Colour.flipABGR(Colour.packRGBA(r, g, b, a)); // We need ABGR so we compose it on the fly
        return this;
    }

    @Override
    public void defaultColor(int r, int g, int b, int a) {
        fixedColor = ((a & 255) << 24) | ((b & 255) << 16) | ((g & 255) << 8) | (r & 255);
        hasFixedColor = true;
    }

    @Override
    public void unsetDefaultColor() {
        hasFixedColor = false;
    }

    @Nonnull
    @Override
    public VertexConsumer uv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Nonnull
    @Override
    public VertexConsumer overlayCoords(int u, int v) {
        return this;
    }

    @Nonnull
    @Override
    public VertexConsumer uv2(int u, int v) {
        light = (v << 16) | u; // Compose lightmap coords into raw light value 0xVVVV_UUUU
        return this;
    }

    @Nonnull
    @Override
    public VertexConsumer normal(float x, float y, float z) {
        nx = x;
        ny = y;
        nz = z;
        return this;
    }

    @Override
    public void endVertex() {
        final Direction dir = Direction.fromNormal((int) nx, (int) ny, (int) nz);
        final int normal = dir != null ? dir.ordinal() : -1;

        // Make sure there is enough space for a new vertex
        if ((this.buffer.capacity() - this.buffer.position()) < VERTEX_SIZE_BYTES) {
            int newCapacity = this.buffer.capacity() * 2;
            if (newCapacity == 0) {
                newCapacity = INITIAL_CAPACITY;
            }
            this.buffer = reallocDirect(this.buffer, newCapacity);
        }

        ByteBuffer buffer = this.buffer;

        // Write the current quad vertex's normal, position, UVs, color and raw light values
        buffer.putInt(normal);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putInt(hasFixedColor ? fixedColor : color);
        buffer.putInt(light);
        // We store 32 bytes per vertex

        resetCurrentVertex(); // Reset the current vertex values
        currentVertex++;
    }

    public void reset() {
        buffer.rewind();
        currentVertex = 0;
        Arrays.fill(sideCount, 0);
        resetCurrentVertex();
    }

    public boolean flush(@Nonnull ChunkModelBuilder buffers, BlockPos origin) {
        if(currentVertex == 0) {
            return false;
        }

        final int numQuads = currentVertex >> 2;

        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            final int normal = buffer.getInt((quadIdx << 2) << 5);
            final Direction dir = normal != -1 ? DirectionUtil.ALL_DIRECTIONS[normal] : null;
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            sideCount[facing.ordinal()]++;
        }

        final int byteSize = currentVertex << 5;
        byte sideMask = 0;

        buffer.rewind();

        ModelVertexSink vertices = buffers.getVertexSink();
        vertices.ensureCapacity(currentVertex);

        final int chunkId = buffers.getChunkId();

        IndexBufferBuilder[] builders = new IndexBufferBuilder[ModelQuadFacing.VALUES.length];

        for(int i = 0; i < ModelQuadFacing.VALUES.length; i++) {
            builders[i] = buffers.getIndexBufferBuilder(ModelQuadFacing.VALUES[i]);
        }

        while (buffer.position() < byteSize) {
            int vertexStart = vertices.getVertexCount();

            final int normal = buffer.getInt(); // Fetch first normal for pre-selecting the vertex sink
            final Direction dir = normal != -1 ? DirectionUtil.ALL_DIRECTIONS[normal] : null;
            final ModelQuadFacing facing = dir != null ? ModelQuadFacing.fromDirection(dir) : ModelQuadFacing.UNASSIGNED;
            final int facingIdx = facing.ordinal();

            writeQuadVertex(vertices, origin, chunkId);
            buffer.getInt();
            writeQuadVertex(vertices, origin, chunkId);
            buffer.getInt();
            writeQuadVertex(vertices, origin, chunkId);
            buffer.getInt();
            writeQuadVertex(vertices, origin, chunkId);

            sideMask |= 1 << facingIdx;

            builders[facingIdx].add(vertexStart, ModelQuadWinding.CLOCKWISE);
        }

        vertices.flush();

        return true;
    }

    private void writeQuadVertex(@Nonnull ModelVertexSink sink, BlockPos origin, int chunkId) {
        final float x = buffer.getFloat();
        final float y = buffer.getFloat();
        final float z = buffer.getFloat();
        final float u = buffer.getFloat();
        final float v = buffer.getFloat();
        final int color = buffer.getInt();
        final int light = buffer.getInt();

        sink.writeVertex(origin, x, y, z, color, u, v, light, chunkId);
    }

    private void resetCurrentVertex() {
        x = y = z = 0F;
        nx = ny = nz = 0F;
        u = v = 0F;
        color = 0xFFFF_FFFF;
        light = 0;
    }
}
