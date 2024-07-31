package me.jellysquid.mods.sodium.client.compat.ccl;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.embeddedt.embeddium.render.frapi.SpriteFinderCache;
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

    private float x;
    private float y;
    private float z;
    private float u;
    private float v;
    private int color;
    private int light;

    private int fixedColor;
    private boolean hasFixedColor = false;

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

        @Override
        public int getComputedFaceNormal() {
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
        return this;
    }

    @Override
    public void endVertex() {
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
        buffer.putInt(hasFixedColor ? fixedColor : color);
        buffer.putInt(light);
        // We store 32 bytes per vertex

        resetCurrentVertex(); // Reset the current vertex values
        currentVertex++;
        if((currentVertex % 4) == 0) {
            recalculateNormals();
        }
    }

    private void recalculateNormals() {
        // Look through the last 4 vertex positions, and compute a proper normal
        ModelQuadFacing normal = ModelQuadUtil.findNormalFace(ModelQuadUtil.calculateNormal(this.previousQuad));
        // Store this in the position for the first vertex of the quad
        buffer.putInt((currentVertex - 4) * VERTEX_SIZE_BYTES, normal.ordinal());
    }

    public void reset() {
        if(currentVertex != 0) {
            buffer.rewind();
            currentVertex = 0;
            Arrays.fill(sideCount, 0);
        }
        resetCurrentVertex();
    }

    public boolean isEmpty() {
        return currentVertex == 0;
    }

    public boolean flush(@Nonnull ChunkModelBuilder buffers, Material material, Vector3fc origin) {
        return flush(buffers, material, origin.x(), origin.y(), origin.z());
    }

    public boolean flush(@Nonnull ChunkModelBuilder buffers, Material material, float oX, float oY, float oZ) {
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

            float midU = 0, midV = 0;

            for(int i = 0; i < 4; i++) {
                if(i != 0)
                    buffer.getInt(); // read normal

                ChunkVertexEncoder.Vertex sodiumVertex = sodiumQuad[i];
                sodiumVertex.x = oX + buffer.getFloat();
                sodiumVertex.y = oY + buffer.getFloat();
                sodiumVertex.z = oZ + buffer.getFloat();
                sodiumVertex.u = buffer.getFloat();
                sodiumVertex.v = buffer.getFloat();
                midU += sodiumVertex.u;
                midV += sodiumVertex.v;
                sodiumVertex.color = buffer.getInt();
                sodiumVertex.light = buffer.getInt();
            }

            // Detect sprite
            TextureAtlasSprite sprite = SpriteFinderCache.forBlockAtlas().findNearestSprite(midU / 4, midV / 4);
            if(sprite != null) {
                buffers.addSprite(sprite);
            }

            sink.push(sodiumQuad, material);

            sideMask |= 1 << facingIdx;
        }

        return true;
    }

    private void resetCurrentVertex() {
        x = y = z = 0F;
        u = v = 0F;
        color = 0xFFFF_FFFF;
        light = 0;
    }
}
