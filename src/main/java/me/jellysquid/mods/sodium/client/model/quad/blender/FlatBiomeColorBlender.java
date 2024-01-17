package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.color.block.BlockColor;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Arrays;

/**
 * A simple colorizer which performs no blending between adjacent blocks.
 */
public class FlatBiomeColorBlender implements BiomeColorBlender {
    private final int[] cachedRet = new int[4];

    @Override
    public int[] getColors(BlockColor colorizer, BlockAndTintGetter world, BlockState state, BlockPos origin,
                           ModelQuadView quad) {
        Arrays.fill(this.cachedRet, ColorARGB.toABGR(colorizer.getColor(state, world, origin, quad.getColorIndex())));

        return this.cachedRet;
    }
}
