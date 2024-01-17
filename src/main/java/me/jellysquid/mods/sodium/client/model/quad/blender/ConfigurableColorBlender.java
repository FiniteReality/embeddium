package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

class ConfigurableColorBlender implements BiomeColorBlender {
    private final BiomeColorBlender defaultBlender;
    private final BiomeColorBlender smoothBlender;

    public ConfigurableColorBlender(Minecraft client) {
        this.defaultBlender = new FlatBiomeColorBlender();
        this.smoothBlender = isSmoothBlendingEnabled(client) ? new SmoothBiomeColorBlender() : this.defaultBlender;
    }

    private static boolean isSmoothBlendingEnabled(Minecraft client) {
        return client.options.biomeBlendRadius > 0;
    }

    @Override
    public int[] getColors(BlockColor colorizer, BlockAndTintGetter world, BlockState state, BlockPos origin,
			ModelQuadView quad) {
    	BiomeColorBlender blender;

        if (BlockColorSettings.isSmoothBlendingEnabled(world, state, origin)) {
            blender = this.smoothBlender;
        } else {
            blender = this.defaultBlender;
        }

        return blender.getColors(colorizer, world, state, origin, quad);
    }

}