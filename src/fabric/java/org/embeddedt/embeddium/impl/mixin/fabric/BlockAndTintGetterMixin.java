package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.level.BlockAndTintGetter;
import org.embeddedt.embeddium.fabric.injectors.BlockAndTintGetterInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockAndTintGetter.class)
public interface BlockAndTintGetterMixin extends BlockAndTintGetterInjector {
}
