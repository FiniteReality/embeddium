package me.jellysquid.mods.sodium.client.world.biome;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.biome.BoxBlur.ColorBuffer;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import java.util.Arrays;

public class BiomeColorCache {
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private final WorldSlice biomeData;

    private final Slice[] slices;
    private final boolean[] populatedSlices;

    private final int blendRadius;

    private final ColorBuffer tempColorBuffer;

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    public BiomeColorCache(WorldSlice biomeData) {
        this.biomeData = biomeData;
        this.blendRadius = Minecraft.getInstance().options.biomeBlendRadius;

        int sizeXZ = 16 + ((NEIGHBOR_BLOCK_RADIUS + this.blendRadius) * 2);
        int sizeY = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

        this.slices = new Slice[sizeY];
        this.populatedSlices = new boolean[sizeY];

        for (int y = 0; y < sizeY; y++) {
            this.slices[y] = new Slice(sizeXZ);
        }

        this.tempColorBuffer = new ColorBuffer(sizeXZ, sizeXZ);
    }

    public void update(ChunkRenderContext context) {
        this.minX = (context.getOrigin().minBlockX() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;
        this.minY = (context.getOrigin().minBlockY() - NEIGHBOR_BLOCK_RADIUS);
        this.minZ = (context.getOrigin().minBlockZ() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;

        this.maxX = (context.getOrigin().maxBlockX() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;
        this.maxY = (context.getOrigin().maxBlockY() + NEIGHBOR_BLOCK_RADIUS);
        this.maxZ = (context.getOrigin().maxBlockZ() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;

        Arrays.fill(this.populatedSlices, false);
    }

    public int getColor(BiomeColorSource source, int blockX, int blockY, int blockZ) {
        int relX = Mth.clamp(blockX, this.minX, this.maxX) - this.minX;
        int relY = Mth.clamp(blockY, this.minY, this.maxY) - this.minY;
        int relZ = Mth.clamp(blockZ, this.minZ, this.maxZ) - this.minZ;

        if (!this.populatedSlices[relY]) {
            this.updateColorBuffers(relY);
        }

        Slice slice = this.slices[relY];
        ColorBuffer buffer = slice.getBuffer(source);

        return buffer.get(relX, relZ);
    }

    private void updateColorBuffers(int relY) {
        Slice slice = this.slices[relY];

        int worldY = this.minY + relY;

        for (int worldZ = this.minZ; worldZ <= this.maxZ; worldZ++) {
            for (int worldX = this.minX; worldX <= this.maxX; worldX++) {
                Biome biome = this.biomeData.getBiome(worldX, worldY, worldZ);

                int relativeX = worldX - this.minX;
                int relativeZ = worldZ - this.minZ;

                slice.grass.set(relativeX, relativeZ, BiomeColors.GRASS_COLOR_RESOLVER.getColor(biome, worldX, worldZ));
                slice.foliage.set(relativeX, relativeZ, BiomeColors.FOLIAGE_COLOR_RESOLVER.getColor(biome, worldX, worldZ));
                slice.water.set(relativeX, relativeZ, BiomeColors.WATER_COLOR_RESOLVER.getColor(biome, worldX, worldZ));
            }
        }

        if (this.blendRadius > 0) {
            BoxBlur.blur(slice.grass, this.tempColorBuffer, this.blendRadius);
            BoxBlur.blur(slice.foliage, this.tempColorBuffer, this.blendRadius);
            BoxBlur.blur(slice.water, this.tempColorBuffer, this.blendRadius);
        }

        this.populatedSlices[relY] = true;
    }

    private static class Slice {
        private final ColorBuffer grass;
        private final ColorBuffer foliage;
        private final ColorBuffer water;

        private Slice(int size) {
            this.grass = new ColorBuffer(size, size);
            this.foliage = new ColorBuffer(size, size);
            this.water = new ColorBuffer(size, size);
        }

        public ColorBuffer getBuffer(BiomeColorSource source) {
            switch (source) {
                case GRASS:
                    return this.grass;
                case FOLIAGE:
                    return this.foliage;
                case WATER:
                    return this.water;
                default:
                    throw new IllegalArgumentException("invalid BiomeColorSource "+source);
            }
        }
    }
}
