package net.neoforged.neoforge.client.model.lighting;

import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;

public abstract class QuadLighter {
    public QuadLighter(BlockColors colors) {
        throw new AssertionError();
    }

    public void setup(BlockAndTintGetter level, BlockPos pos, BlockState blockState) {
        throw new AssertionError();
    }

    public void computeLightingForQuad(int[] vertexData, boolean shade) {
        throw new AssertionError();
    }

    public void computeLightingForQuad(BakedQuad forgeQuad) {
        throw new AssertionError();
    }

    public int[] getComputedLightmap() {
        throw new AssertionError();
    }

    public float[] getComputedBrightness() {
        throw new AssertionError();
    }
}
