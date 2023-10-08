package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.format.ChunkModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;

public record ChunkShaderOptions(ChunkFogMode fog, BlockRenderPass pass, ChunkVertexType vertexType) {
    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.getAlphaCutoff() != 0.0f) {
            constants.add("ALPHA_CUTOFF", String.valueOf(this.pass.getAlphaCutoff()));
        }

        if(vertexType != ChunkModelVertexFormats.VANILLA_LIKE)
            constants.add("USE_VERTEX_COMPRESSION");
        constants.add("VERT_POS_SCALE", String.valueOf(this.vertexType.getPositionScale()));
        constants.add("VERT_POS_OFFSET", String.valueOf(this.vertexType.getPositionOffset()));
        constants.add("VERT_TEX_SCALE", String.valueOf(this.vertexType.getTextureScale()));

        return constants.build();
    }
}
