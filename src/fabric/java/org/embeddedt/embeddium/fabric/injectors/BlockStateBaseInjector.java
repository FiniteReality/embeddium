package org.embeddedt.embeddium.fabric.injectors;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

public interface BlockStateBaseInjector {
    default boolean isEmpty() {
        return ((BlockBehaviour.BlockStateBase)this).isAir();
    }

    default int getLightEmission(BlockGetter getter, BlockPos pos) {
        return ((BlockBehaviour.BlockStateBase)this).getLightEmission();
    }

    default boolean shouldDisplayFluidOverlay(BlockGetter world, BlockPos pos, FluidState fluidState) {
        Block block = world.getBlockState(pos).getBlock();
        return block instanceof net.minecraft.world.level.block.HalfTransparentBlock || block instanceof net.minecraft.world.level.block.LeavesBlock;
    }

    default boolean hidesNeighborFace(BlockGetter view, BlockPos pos, BlockState selfState, Direction opposite) {
        return false;
    }

    default boolean supportsExternalFaceHiding() {
       return false;
    }
}
