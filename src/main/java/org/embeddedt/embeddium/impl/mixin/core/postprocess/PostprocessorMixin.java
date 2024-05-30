package org.embeddedt.embeddium.impl.mixin.core.postprocess;

import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.world.cloned.ClonedChunkSection;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = { WorldSlice.class, ClonedChunkSection.class }, remap = false)
public class PostprocessorMixin {
}
