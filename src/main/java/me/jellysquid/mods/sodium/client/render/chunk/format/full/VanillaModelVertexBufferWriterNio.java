package me.jellysquid.mods.sodium.client.render.chunk.format.full;

import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferView;
import me.jellysquid.mods.sodium.client.model.vertex.buffer.VertexBufferWriterNio;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

import java.nio.ByteBuffer;

public class VanillaModelVertexBufferWriterNio extends VertexBufferWriterNio implements ModelVertexSink {
    public VanillaModelVertexBufferWriterNio(VertexBufferView backingBuffer) {
        super(backingBuffer, ChunkModelVertexFormats.VANILLA_LIKE);
    }

    @Override
    public void writeVertex(float posX, float posY, float posZ, int color, float u, float v, int light, int chunkId) {
        int i = this.writeOffset;

        ByteBuffer buffer = this.byteBuffer;
        buffer.putFloat(i + 0, posX);
        buffer.putFloat(i + 4, posY);
        buffer.putFloat(i + 8, posZ);
        buffer.putInt(i + 12, color);

        buffer.putShort(i + 16, VanillaModelVertexType.encodeBlockTexture(u));
        buffer.putShort(i + 18, VanillaModelVertexType.encodeBlockTexture(v));
        buffer.putShort(i + 20, (short) chunkId);

        buffer.putInt(i + 24, VanillaModelVertexType.encodeLightMapTexCoord(light));

        this.advance();
    }
}
