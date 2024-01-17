package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.render.vanilla.block.BlockColorSettings;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.StateHolder;

class ConfigurableColorBlender implements ColorBlender {
    private final ColorBlender defaultBlender;
    private final ColorBlender smoothBlender;

    public ConfigurableColorBlender(Minecraft client) {
        this.defaultBlender = new FlatColorBlender();
        this.smoothBlender = isSmoothBlendingEnabled(client) ? new LinearColorBlender() : this.defaultBlender;
    }

    private static boolean isSmoothBlendingEnabled(Minecraft client) {
        return client.options.biomeBlendRadius().get() > 0;
    }

    @Override
    public <T extends StateHolder<O, ?>, O> int[] getColors(BlockAndTintGetter world, BlockPos origin, ModelQuadView quad, ColorSampler<T> sampler, T state) {
        ColorBlender blender;

        if (BlockColorSettings.isSmoothBlendingEnabled(world, state, origin)) {
            blender = this.smoothBlender;
        } else {
            blender = this.defaultBlender;
        }

        return blender.getColors(world, origin, quad, sampler, state);
    }
}