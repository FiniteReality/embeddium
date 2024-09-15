package me.jellysquid.mods.sodium.mixin.core.world.biome;

import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeZoomer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(BiomeManager.class)
public interface BiomeManagerAccessor {
    @Accessor
    BiomeZoomer getZoomer();
}
