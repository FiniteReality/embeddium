package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.level.material.Fluid;
import org.embeddedt.embeddium.fabric.injectors.FluidInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Fluid.class)
public class FluidMixin implements FluidInjector {
}
