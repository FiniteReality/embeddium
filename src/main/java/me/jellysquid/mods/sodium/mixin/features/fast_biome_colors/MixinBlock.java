package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import org.spongepowered.asm.mixin.Mixin;

import me.jellysquid.mods.sodium.client.render.vanilla.block.BlockColorSettings;
import me.jellysquid.mods.sodium.client.render.vanilla.block.DefaultBlockColorSettings;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

@Mixin(Block.class)
public class MixinBlock implements BlockColorSettings<BlockState> {
    @Override
    public boolean useSmoothColorBlending(BlockAndTintGetter view, BlockState state, BlockPos pos) {
        return DefaultBlockColorSettings.isSmoothBlendingAvailable(state.getBlock());
    }
}