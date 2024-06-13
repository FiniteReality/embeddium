package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.level.block.entity.BlockEntity;
import org.embeddedt.embeddium.fabric.injectors.BlockEntityInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntity.class)
public class BlockEntityMixin implements BlockEntityInjector {
}
