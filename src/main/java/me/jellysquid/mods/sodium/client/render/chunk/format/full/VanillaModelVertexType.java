package me.jellysquid.mods.sodium.client.render.chunk.format.full;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.type.BlittableVertexType;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.render.chunk.format.VanillaLikeChunkMeshAttribute;

/**
 * Simple vertex format which uses single-precision floating point numbers to represent position and texture
 * coordinates.
 */
public class VanillaModelVertexType implements ChunkVertexType<VanillaLikeChunkMeshAttribute> {
    public static final GlVertexFormat<VanillaLikeChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(VanillaLikeChunkMeshAttribute.class, 28)
            .addElement(VanillaLikeChunkMeshAttribute.POSITION, 0, GlVertexAttributeFormat.FLOAT, 3, false, false)
            .addElement(VanillaLikeChunkMeshAttribute.COLOR, 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(VanillaLikeChunkMeshAttribute.BLOCK_TEX_ID, 16, GlVertexAttributeFormat.FLOAT, 2, false, false)
            .addElement(VanillaLikeChunkMeshAttribute.LIGHT, 24, GlVertexAttributeFormat.UNSIGNED_INT, 1, false, true)
            .build();

    @Override
    public ModelVertexSink createFallbackWriter(VertexConsumer consumer) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ModelVertexSink createBufferWriter(VertexBufferView buffer, boolean direct) {
        return direct ? new VanillaModelVertexBufferWriterUnsafe(buffer) : new VanillaModelVertexBufferWriterNio(buffer);
    }

    @Override
    public BlittableVertexType<ModelVertexSink> asBlittable() {
        return this;
    }

    @Override
    public GlVertexFormat<VanillaLikeChunkMeshAttribute> getCustomVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public float getTextureScale() {
        return 1f;
    }

    static float encodeBlockTexture(float value) {
        return Math.min(0.99999997F, value);
    }

    static int encodeLight(int light) {
        int block = light & 0xFF;
        int sky = (light >> 16) & 0xFF;
        return ((block << 0) | (sky << 8));
    }

    @Override
    public float getPositionOffset() {
        return 0;
    }

    @Override
    public float getPositionScale() {
        return 1;
    }
}
