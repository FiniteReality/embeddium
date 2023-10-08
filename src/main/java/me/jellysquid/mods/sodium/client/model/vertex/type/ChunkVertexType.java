package me.jellysquid.mods.sodium.client.model.vertex.type;

import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;

public interface ChunkVertexType<A extends Enum<A>> extends BlittableVertexType<ModelVertexSink>, CustomVertexType<ModelVertexSink, A> {
    /**
     * @return The scale to be applied to vertex coordinates
     */
    float getPositionScale();

    /**
     * @return The translation to be applied to vertex coordinates
     */
    float getPositionOffset();

    /**
     * @return The scale to be applied to texture coordinates
     */
    float getTextureScale();
}
