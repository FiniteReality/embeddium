package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;

public interface ChunkVertexType {
    GlVertexFormat<?> getVertexFormat();

    ChunkVertexEncoder getEncoder();

    String getDefine();
}
