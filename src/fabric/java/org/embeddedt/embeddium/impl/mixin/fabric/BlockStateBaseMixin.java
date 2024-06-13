package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.level.block.state.BlockBehaviour;
import org.embeddedt.embeddium.fabric.injectors.BlockStateBaseInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockBehaviour.BlockStateBase.class)
public class BlockStateBaseMixin implements BlockStateBaseInjector {
}
