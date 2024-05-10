package me.jellysquid.mods.sodium.client.model.quad.blender;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;

import java.util.Arrays;

public abstract class BlendedColorProvider<T> implements ColorProvider<T> {
    private static boolean shouldUseVertexBlending;

    private final BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

    public static void checkBlendingEnabled() {
        shouldUseVertexBlending = Minecraft.getInstance().options.biomeBlendRadius().get() > 0;
    }

    @Override
    public void getColors(WorldSlice view, BlockPos pos, T state, ModelQuadView quad, int[] output) {
        if (shouldUseVertexBlending) {
            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                output[vertexIndex] = this.getVertexColor(view, pos, quad, vertexIndex);
            }
        } else {
            // Just sample the exact position of that block (like vanilla), and use the same color on all vertices
            Arrays.fill(output, ColorARGB.toABGR(this.getColor(view, pos)));
        }
    }

    private int getVertexColor(WorldSlice world, BlockPos blockPos, ModelQuadView quad, int vertexIndex) {
        // Offset the position by -0.5f to align smooth blending with flat blending.
        final float posX = quad.getX(vertexIndex) - 0.5f;
        final float posY = quad.getY(vertexIndex) - 0.5f;
        final float posZ = quad.getZ(vertexIndex) - 0.5f;

        // Floor the positions here to always get the largest integer below the input
        // as negative values by default round toward zero when casting to an integer.
        // Which would cause negative ratios to be calculated in the interpolation later on.
        final int intX = Mth.floor(posX);
        final int intY = Mth.floor(posY);
        final int intZ = Mth.floor(posZ);

        // Integer component of position vector
        final int worldIntX = blockPos.getX() + intX;
        final int worldIntY = blockPos.getY() + intY;
        final int worldIntZ = blockPos.getZ() + intZ;

        var neighborPos = cursor;

        // Retrieve the color values for each neighboring block
        final int c00 = this.getColor(world, neighborPos.set(worldIntX + 0, worldIntY, worldIntZ + 0));
        final int c01 = this.getColor(world, neighborPos.set(worldIntX + 0, worldIntY, worldIntZ + 1));
        final int c10 = this.getColor(world, neighborPos.set(worldIntX + 1, worldIntY, worldIntZ + 0));
        final int c11 = this.getColor(world, neighborPos.set(worldIntX + 1, worldIntY, worldIntZ + 1));

        // Linear interpolation across the Z-axis
        int z0;

        if (c00 != c01) {
            z0 = ColorMixer.mix(c00, c01, posZ - intZ);
        } else {
            z0 = c00;
        }

        int z1;

        if (c10 != c11) {
            z1 = ColorMixer.mix(c10, c11, posZ - intZ);
        } else {
            z1 = c10;
        }

        // Linear interpolation across the X-axis
        int x0;

        if (z0 != z1) {
            x0 = ColorMixer.mix(z0, z1, posX - intX);
        } else {
            x0 = z0;
        }

        return ColorARGB.toABGR(x0);
    }

    protected int getColor(WorldSlice world, BlockPos pos) {
        return getColor(world, pos.getX(), pos.getY(), pos.getZ());
    }

    @Deprecated(forRemoval = true)
    protected int getColor(WorldSlice world, int x, int y, int z) {
        throw new AssertionError("Must override one of the getColor methods");
    }
}
