package me.jellysquid.mods.sodium.mixin.features.fast_biome_colors;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;
import org.spongepowered.asm.mixin.Mixin;

import me.jellysquid.mods.sodium.client.render.vanilla.block.BlockColorSettings;
import me.jellysquid.mods.sodium.client.render.vanilla.block.DefaultBlockColorSettings;

@Mixin(Block.class)
public class MixinBlock implements BlockColorSettings<BlockState> {
    @Override
    public boolean useSmoothColorBlending(BlockRenderView view, BlockState state, BlockPos pos) {
        return DefaultBlockColorSettings.isSmoothBlendingAvailable(state.getBlock());
    }
}