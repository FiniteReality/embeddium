package me.jellysquid.mods.sodium.client.world.biome;

import net.minecraft.client.color.block.BlockColor;
import net.minecraft.world.level.block.state.BlockState;

public interface BlockColorsExtended {
    BlockColor getColorProvider(BlockState state);
}
