package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.level.chunk.LevelChunk;
import org.embeddedt.embeddium.fabric.injectors.LevelChunkInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LevelChunk.class)
public class LevelChunkMixin implements LevelChunkInjector {
}
