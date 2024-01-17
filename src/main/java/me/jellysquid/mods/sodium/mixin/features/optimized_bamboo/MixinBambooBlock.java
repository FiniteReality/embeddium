package me.jellysquid.mods.sodium.mixin.features.optimized_bamboo;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BambooBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BambooBlock.class)
public class MixinBambooBlock extends Block {
    public MixinBambooBlock(Properties settings) {
        super(settings);
    }

    // This is a fix for an oversight on Mojang's side, where this block always returns 1.0 regardless of the state. It returns the same result,
    // but improves performance significantly. This was originally found by darkevilmac in https://github.com/TridentMC/FastBamboo.
    @Override
    public float getShadeBrightness(BlockState state, BlockGetter world, BlockPos pos) {
        return 1.0f;
    }
}