package net.neoforged.neoforge.fluids;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;

public class FluidType {
    public static final FluidType VANILLA = new FluidType();

    public int getLightLevel(FluidState state, BlockAndTintGetter getter, BlockPos pos) {
        return state.getType() instanceof LavaFluid ? 15 : 0;
    }
}
