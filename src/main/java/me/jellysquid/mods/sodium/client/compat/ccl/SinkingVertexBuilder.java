package me.jellysquid.mods.sodium.client.compat.ccl;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

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

    private float x = Float.NaN;
    private float y;
    private float z;
    private float u;
    private float v;
    private int color;
    private int light;

    private final ChunkVertexEncoder.Vertex[] sodiumVertexArray = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final ModelQuadView previousQuad = new ModelQuadView() {

        /**
         * @param idx the index of the desired vertex
         * @param offset the offset into that vertex, as an integer
         * @return appropriate byte offset
         */
        private int getBaseIndex(int idx, int offset) {
            return (currentVertex - 4 + idx) * VERTEX_SIZE_BYTES + (offset * 4);
        }

        @Override
        public float getX(int idx) {
            return buffer.getFloat(getBaseIndex(idx, 1));
        }

        @Override
        public float getY(int idx) {
            return buffer.getFloat(getBaseIndex(idx, 2));
        }

        @Override
        public float getZ(int idx) {
            return buffer.getFloat(getBaseIndex(idx, 3));
        }

        @Override
        public int getColor(int idx) {
            return buffer.getInt(getBaseIndex(idx, 4));
        }

        @Override
        public float getTexU(int idx) {
            return buffer.getFloat(getBaseIndex(idx, 5));
        }

        @Override
        public float getTexV(int idx) {
            return buffer.getFloat(getBaseIndex(idx, 6));
        }

        @Override
        public int getLight(int idx) {
            return buffer.getInt(getBaseIndex(idx, 7));
        }

        @Override
        public int getFlags() {
            return 0;
        }

        @Override
        public int getColorIndex() {
            return 0;
        }

        @Override
        public TextureAtlasSprite getSprite() {
            return null;
        }

        @Override
        public Direction getLightFace() {
            return null;
        }

        @Override
        public int getForgeNormal(int idx) {
            return 0;
        }
    };

    private static ByteBuffer reallocDirect(ByteBuffer old, int capacity) {
        ByteBuffer newBuf = ByteBuffer.allocateDirect(capacity).order(ByteOrder.nativeOrder());
        int oldPos = old.position();
        old.rewind();
        newBuf.put(old);
        newBuf.position(Math.min(capacity, oldPos));
        old.position(oldPos);
        return newBuf;
    }

    @Override
    public VertexConsumer addVertex(float x, float y, float z) {
        pushLastVertex();
        this.x = x;
        this.y = y;
        this.z = z;
        return this;
    }

    @Nonnull
    @Override
    public VertexConsumer setColor(int r, int g, int b, int a) {
        color = ((a & 255) << 24) | ((b & 255) << 16) | ((g & 255) << 8) | (r & 255);
        // Colour.flipABGR(Colour.packRGBA(r, g, b, a)); // We need ABGR so we compose it on the fly
        return this;
    }

    @Override
    public VertexConsumer setUv(float u, float v) {
        this.u = u;
        this.v = v;
        return this;
    }

    @Override
    public VertexConsumer setUv1(int p_350815_, int p_350629_) {
        return this;
    }

    @Override
    public VertexConsumer setUv2(int p_350859_, int p_351004_) {
        this.light = (p_351004_ << 16) | (p_350859_ & 0xFFFF);
        return this;
    }

    @Override
    public VertexConsumer setLight(int light) {
        this.light = light;
        return this;
    }

    @Override
    public VertexConsumer setNormal(float p_350429_, float p_350286_, float p_350836_) {
        return this;
    }

    private void pushLastVertex() {
        if(Float.isNaN(this.x)) {
            return;
        }
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
        buffer.putInt(-1);
        buffer.putFloat(x);
        buffer.putFloat(y);
        buffer.putFloat(z);
        buffer.putFloat(u);
        buffer.putFloat(v);
        buffer.putInt(color);
        buffer.putInt(light);
        // We store 32 bytes per vertex

        resetCurrentVertex(); // Reset the current vertex values
        currentVertex++;
        if((currentVertex % 4) == 0) {
            recalculateNormals();
        }
        this.x = Float.NaN;
    }

    private void recalculateNormals() {
        // Look through the last 4 vertex positions, and compute a proper normal
        ModelQuadFacing normal = ModelQuadUtil.findNormalFace(ModelQuadUtil.calculateNormal(this.previousQuad));
        // Store this in the position for the first vertex of the quad
        buffer.putInt((currentVertex - 4) * VERTEX_SIZE_BYTES, normal.ordinal());
    }

    public void reset() {
        buffer.rewind();
        currentVertex = 0;
        Arrays.fill(sideCount, 0);
        resetCurrentVertex();
    }

    public boolean flush(@Nonnull ChunkModelBuilder buffers, Material material, Vector3fc origin) {
        return flush(buffers, material, origin.x(), origin.y(), origin.z());
    }

    public boolean flush(@Nonnull ChunkModelBuilder buffers, Material material, float oX, float oY, float oZ) {
        pushLastVertex();
        if(currentVertex == 0) {
            return false;
        }

        final int numQuads = currentVertex >> 2;

        for (int quadIdx = 0; quadIdx < numQuads; quadIdx++) {
            final int normal = buffer.getInt((quadIdx << 2) << 5);
            sideCount[normal]++;
        }

        /*
        for (final ModelQuadFacing facing : ModelQuadFacing.VALUES) {
            final int count = sideCount[facing.ordinal()];
            if (count == 0) {
                continue;
            }
            buffers.getVertexBuffer(facing).ensureCapacity(count << 2);
        }

         */

        final int byteSize = currentVertex << 5;
        byte sideMask = 0;

        buffer.rewind();

        while (buffer.position() < byteSize) {
            final int normal = buffer.getInt(); // Fetch first normal for pre-selecting the vertex sink
            final ModelQuadFacing facing = ModelQuadFacing.VALUES[normal];
            final int facingIdx = facing.ordinal();

            final ChunkMeshBufferBuilder sink = buffers.getVertexBuffer(facing);

            ChunkVertexEncoder.Vertex[] sodiumQuad = sodiumVertexArray;

            for(int i = 0; i < 4; i++) {
                if(i != 0)
                    buffer.getInt(); // read normal

                ChunkVertexEncoder.Vertex sodiumVertex = sodiumQuad[i];
                sodiumVertex.x = oX + buffer.getFloat();
                sodiumVertex.y = oY + buffer.getFloat();
                sodiumVertex.z = oZ + buffer.getFloat();
                sodiumVertex.u = buffer.getFloat();
                sodiumVertex.v = buffer.getFloat();
                sodiumVertex.color = buffer.getInt();
                sodiumVertex.light = buffer.getInt();
            }

            sink.push(sodiumQuad, material);

            sideMask |= 1 << facingIdx;
        }

        return true;
    }

    private void resetCurrentVertex() {
        x = y = z = Float.NaN; // we use NaN to indicate that there is no started vertex
        u = v = 0F;
        color = 0xFFFF_FFFF;
        light = 0;
    }
}
