package me.jellysquid.mods.sodium.client.world;

import net.minecraft.client.multiplayer.ClientLevel;

public interface BiomeSeedProvider {
    long getBiomeSeed();

    static long getBiomeSeed(ClientLevel world) {
        return ((BiomeSeedProvider)world).getBiomeSeed();
    }
}
