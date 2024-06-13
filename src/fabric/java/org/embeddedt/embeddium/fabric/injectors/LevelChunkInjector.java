package org.embeddedt.embeddium.fabric.injectors;

import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;

public interface LevelChunkInjector {
    default AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return null;
    }
}
