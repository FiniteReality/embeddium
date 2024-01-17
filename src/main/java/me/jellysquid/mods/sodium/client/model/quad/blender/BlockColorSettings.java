package me.jellysquid.mods.sodium.client.model.quad.blender;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.StateHolder;

public interface BlockColorSettings<T> {
    /**
     * Configures whether biome colors from a color provider will be interpolated for this block. You should only
     * enable this functionality if your color provider returns values based upon a pair of coordinates in the world,
     * and not if it needs access to the block state itself.
     *
     * @return True if interpolation should be used, otherwise false.
     */
    boolean useSmoothColorBlending(BlockAndTintGetter view, T state, BlockPos pos);

    @SuppressWarnings("unchecked")
    static <T> boolean isSmoothBlendingEnabled(BlockAndTintGetter world, StateHolder<T, ?> state, BlockPos pos) {
        if (state.owner instanceof BlockColorSettings) {
        	BlockColorSettings<StateHolder<T, ?>> settings = (BlockColorSettings<StateHolder<T, ?>>) state.owner;
            return settings.useSmoothColorBlending(world, state, pos);
        }

        return false;
    }
}