package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.level.chunk.ChunkBiomeContainer;

public interface ChunkBiomeContainerExtended {
    static ChunkBiomeContainer clone(ChunkBiomeContainer container) {
        return container != null ? ((ChunkBiomeContainerExtended)container).embeddium$copy() : null;
    }

    ChunkBiomeContainer embeddium$copy();
}
