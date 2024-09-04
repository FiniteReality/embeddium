package me.jellysquid.mods.sodium.mixin.core.postprocess;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = { WorldSlice.class, ClonedChunkSection.class }, remap = false)
public class PostprocessorMixin {
}
