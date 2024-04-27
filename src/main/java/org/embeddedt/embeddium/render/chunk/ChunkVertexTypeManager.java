package org.embeddedt.embeddium.render.chunk;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;

import java.util.Objects;

public class ChunkVertexTypeManager {
    private static ChunkVertexType currentType;

    public static void updateType() {
        ChunkVertexType vertexType;

        if (RenderSectionManager.USE_CORE_SHADERS) {
            vertexType = ChunkMeshFormats.VANILLA;
        } else if (SodiumClientMod.canUseVanillaVertices()) {
            vertexType = ChunkMeshFormats.VANILLA_LIKE;
        } else {
            vertexType = ChunkMeshFormats.COMPACT;
        }

        currentType = vertexType;
    }

    public static ChunkVertexType getType() {
        return Objects.requireNonNull(currentType);
    }
}
