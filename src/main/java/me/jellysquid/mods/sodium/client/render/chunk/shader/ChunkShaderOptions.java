package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.ShaderConstants;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import org.embeddedt.embeddium.render.ShaderModBridge;

public record ChunkShaderOptions(ChunkFogMode fog, TerrainRenderPass pass, ChunkVertexType vertexType) {
    /**
     * @deprecated Only kept for Iris/Oculus compatibility, do not use
     */
    @Deprecated
    @SuppressWarnings("unused")
    public ChunkShaderOptions(ChunkFogMode fog, TerrainRenderPass pass) {
        this(fog, pass, ChunkMeshFormats.COMPACT);
    }

    public ShaderConstants constants() {
        ShaderConstants.Builder constants = ShaderConstants.builder();
        constants.addAll(this.fog.getDefines());

        if (this.pass.supportsFragmentDiscard()) {
            constants.add("USE_FRAGMENT_DISCARD");
        }

        // Embeddium: indicate whether compact vertex format is disabled to shaders
        if(this.vertexType != ChunkMeshFormats.VANILLA_LIKE) {
            constants.add("USE_VERTEX_COMPRESSION");
        }

        constants.add("VERT_POS_SCALE", String.valueOf(this.vertexType.getPositionScale()));
        constants.add("VERT_POS_OFFSET", String.valueOf(this.vertexType.getPositionOffset()));
        constants.add("VERT_TEX_SCALE", String.valueOf(this.vertexType.getTextureScale()));

        if(!ShaderModBridge.emulateLegacyColorBrightnessFormat()) {
            constants.add("USE_VANILLA_COLOR_FORMAT");
        }

        return constants.build();
    }
}
