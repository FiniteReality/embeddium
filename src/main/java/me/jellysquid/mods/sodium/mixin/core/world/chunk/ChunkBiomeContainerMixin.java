package me.jellysquid.mods.sodium.mixin.core.world.chunk;

import me.jellysquid.mods.sodium.client.world.ChunkBiomeContainerExtended;
import net.minecraft.core.IdMap;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkBiomeContainer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ChunkBiomeContainer.class)
public class ChunkBiomeContainerMixin implements ChunkBiomeContainerExtended {
    @Shadow
    @Final
    private IdMap<Biome> biomeRegistry;

    @Shadow
    @Final
    private Biome[] biomes;

    @Override
    public ChunkBiomeContainer embeddium$copy() {
        return new ChunkBiomeContainer(this.biomeRegistry, this.biomes.clone());
    }
}
