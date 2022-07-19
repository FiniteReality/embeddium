package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;

import me.jellysquid.mods.sodium.client.render.vanilla.block.BlockColorSettings;
import me.jellysquid.mods.sodium.client.render.vanilla.block.DefaultBlockColorSettings;

@Mixin(Fluid.class)
public class MixinFluid implements BlockColorSettings<FluidState> {
    @Override
    public boolean useSmoothColorBlending(BlockRenderView view, FluidState state, BlockPos pos) {
        return DefaultBlockColorSettings.isSmoothBlendingAvailable(state.getFluid());
    }
}