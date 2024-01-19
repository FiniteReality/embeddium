package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeZoomer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeManager.class)
public interface AccessorBiomeManager {
    @Accessor("zoomer")
    BiomeZoomer getZoomer();
}
