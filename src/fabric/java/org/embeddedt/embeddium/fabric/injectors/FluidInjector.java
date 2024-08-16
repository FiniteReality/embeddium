package org.embeddedt.embeddium.fabric.injectors;

import net.neoforged.neoforge.fluids.FluidType;

public interface FluidInjector {
    default FluidType getFluidType() {
        return FluidType.VANILLA;
    }
}
