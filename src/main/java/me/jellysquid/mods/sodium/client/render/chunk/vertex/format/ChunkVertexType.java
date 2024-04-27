package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;

import java.util.List;

public interface ChunkVertexType {
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

    GlVertexFormat<ChunkMeshAttribute> getVertexFormat();

    ChunkVertexEncoder getEncoder();

    default GlVertexAttributeBinding[] getAttributeBindings() {
        var vertexFormat = getVertexFormat();
        return new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                        vertexFormat.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        vertexFormat.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                        vertexFormat.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                        vertexFormat.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE))
        };
    }

    // TODO post-BC: make this have no default impl
    default List<String> getShaderDefines() {
        return List.of("USE_VERTEX_COMPRESSION");
    }
}
