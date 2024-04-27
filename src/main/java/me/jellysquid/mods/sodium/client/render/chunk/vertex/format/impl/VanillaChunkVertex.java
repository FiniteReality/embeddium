package me.jellysquid.mods.sodium.client.render.chunk.vertex.format.impl;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeBinding;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexAttributeFormat;
import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.render.chunk.shader.ChunkShaderBindingPoints;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshAttribute;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import org.lwjgl.system.MemoryUtil;

import java.util.List;

/**
 * This vertex format uses the same format as {@link com.mojang.blaze3d.vertex.DefaultVertexFormat#BLOCK}. It is not
 * intended to be used with Embeddium's shaders.
 */
public class VanillaChunkVertex implements ChunkVertexType {
    public static final int STRIDE = 32;

    public static final GlVertexFormat<ChunkMeshAttribute> VERTEX_FORMAT = GlVertexFormat.builder(ChunkMeshAttribute.class, STRIDE)
            .addElement(ChunkMeshAttribute.POSITION_MATERIAL_MESH, 0, GlVertexAttributeFormat.FLOAT, 3, false, false)
            .addElement(ChunkMeshAttribute.COLOR_SHADE, 12, GlVertexAttributeFormat.UNSIGNED_BYTE, 4, true, false)
            .addElement(ChunkMeshAttribute.BLOCK_TEXTURE, 16, GlVertexAttributeFormat.FLOAT, 2, false, false)
            .addElement(ChunkMeshAttribute.LIGHT_TEXTURE, 24, GlVertexAttributeFormat.UNSIGNED_SHORT, 2, false, true)
            .addElement(ChunkMeshAttribute.NORMAL, 28, GlVertexAttributeFormat.SIGNED_BYTE, 3, true, false)
            .build();

    @Override
    public float getPositionScale() {
        return 1f;
    }

    @Override
    public float getPositionOffset() {
        return 0;
    }

    @Override
    public float getTextureScale() {
        return 1f;
    }

    @Override
    public GlVertexFormat<ChunkMeshAttribute> getVertexFormat() {
        return VERTEX_FORMAT;
    }

    @Override
    public ChunkVertexEncoder getEncoder() {
        return (ptr, material, vertex, sectionIndex) -> {
            MemoryUtil.memPutFloat(ptr + 0, vertex.x);
            MemoryUtil.memPutFloat(ptr + 4, vertex.y);
            MemoryUtil.memPutFloat(ptr + 8, vertex.z);

            // Unpack brightness and apply it to the other three color values to match vanilla format
            int c = vertex.color;
            MemoryUtil.memPutInt(ptr + 12, ColorABGR.withAlpha(ColorMixer.mulSingle(c, ColorABGR.unpackAlpha(c)), 255));
            MemoryUtil.memPutFloat(ptr + 16, encodeTexture(vertex.u));
            MemoryUtil.memPutFloat(ptr + 20, encodeTexture(vertex.v));
            MemoryUtil.memPutInt(ptr + 24, vertex.light);
            MemoryUtil.memPutInt(ptr + 28, vertex.normal);

            return ptr + STRIDE;
        };
    }

    @Override
    public GlVertexAttributeBinding[] getAttributeBindings() {
        return new GlVertexAttributeBinding[] {
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_POSITION_ID,
                        VERTEX_FORMAT.getAttribute(ChunkMeshAttribute.POSITION_MATERIAL_MESH)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_COLOR,
                        VERTEX_FORMAT.getAttribute(ChunkMeshAttribute.COLOR_SHADE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_BLOCK_TEXTURE,
                        VERTEX_FORMAT.getAttribute(ChunkMeshAttribute.BLOCK_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_LIGHT_TEXTURE,
                        VERTEX_FORMAT.getAttribute(ChunkMeshAttribute.LIGHT_TEXTURE)),
                new GlVertexAttributeBinding(ChunkShaderBindingPoints.ATTRIBUTE_NORMAL,
                        VERTEX_FORMAT.getAttribute(ChunkMeshAttribute.NORMAL))
        };
    }

    private static float encodeTexture(float value) {
        return Math.min(0.99999997F, value);
    }

    @Override
    public List<String> getShaderDefines() {
        return List.of();
    }
}
