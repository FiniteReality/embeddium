package me.jellysquid.mods.sodium.client.world.biome;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.world.biome.BoxBlur.ColorBuffer;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.biome.Biome;

import java.util.Arrays;

public class BiomeColorCache {
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;
    private final BiomeSlice biomeData;

    private final Reference2ReferenceOpenHashMap<ColorResolver, Slice[]> slices;

    private final int blendRadius;

    private final ColorBuffer tempColorBuffer;

    private int minX, minY, minZ;
    private int maxX, maxY, maxZ;

    private int populateStamp;

    private final int sizeXZ, sizeY;


    public BiomeColorCache(BiomeSlice biomeData, int blendRadius) {
        this.biomeData = biomeData;
        this.blendRadius = blendRadius;

        this.sizeXZ = 16 + ((NEIGHBOR_BLOCK_RADIUS + this.blendRadius) * 2);
        this.sizeY = 16 + (NEIGHBOR_BLOCK_RADIUS * 2);

        this.slices = new Reference2ReferenceOpenHashMap<>();
        this.populateStamp = 1;

        this.tempColorBuffer = new ColorBuffer(sizeXZ, sizeXZ);
    }

    public void update(ChunkRenderContext context) {
        this.minX = (context.getOrigin().minBlockX() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;
        this.minY = (context.getOrigin().minBlockY() - NEIGHBOR_BLOCK_RADIUS);
        this.minZ = (context.getOrigin().minBlockZ() - NEIGHBOR_BLOCK_RADIUS) - this.blendRadius;

        this.maxX = (context.getOrigin().maxBlockX() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;
        this.maxY = (context.getOrigin().maxBlockY() + NEIGHBOR_BLOCK_RADIUS);
        this.maxZ = (context.getOrigin().maxBlockZ() + NEIGHBOR_BLOCK_RADIUS) + this.blendRadius;

        this.populateStamp++;
    }

    public int getColor(ColorResolver resolver, int blockX, int blockY, int blockZ) {
        int relX = Mth.clamp(blockX, this.minX, this.maxX) - this.minX;
        int relY = Mth.clamp(blockY, this.minY, this.maxY) - this.minY;
        int relZ = Mth.clamp(blockZ, this.minZ, this.maxZ) - this.minZ;

        if (!this.slices.containsKey(resolver)) {
            this.initializeSlices(resolver);
        }

        Slice slice = this.slices.get(resolver)[relY];

        if (slice.lastPopulateStamp < this.populateStamp) {
            this.updateColorBuffers(relY, resolver, slice);
        }

        ColorBuffer buffer = slice.getBuffer();

        return buffer.get(relX, relZ);
    }

    private void initializeSlices(ColorResolver resolver) {
        Slice[] slice = new Slice[this.sizeY];
        this.slices.put(resolver, slice);

        for (int y = 0; y < this.sizeY; y++) {
            slice[y] = new Slice(this.sizeXZ);
        }
    }

    private void updateColorBuffers(int relY, ColorResolver resolver, Slice slice) {
        int worldY = this.minY + relY;

        for (int worldZ = this.minZ; worldZ <= this.maxZ; worldZ++) {
            for (int worldX = this.minX; worldX <= this.maxX; worldX++) {
                Biome biome = this.biomeData.getBiome(worldX, worldY, worldZ);

                int relativeX = worldX - this.minX;
                int relativeZ = worldZ - this.minZ;

                slice.buffer.set(relativeX, relativeZ, resolver.getColor(biome, worldX, worldZ));
            }
        }

        if (this.blendRadius > 0) {
            BoxBlur.blur(slice.buffer, this.tempColorBuffer, this.blendRadius);
        }

        slice.lastPopulateStamp = this.populateStamp;
    }

    private static class Slice {
        private final ColorBuffer buffer;
        private long lastPopulateStamp;

        private Slice(int size) {
            this.buffer = new ColorBuffer(size, size);
            this.lastPopulateStamp = 0;
        }

        public ColorBuffer getBuffer() {
            return this.buffer;
        }
    }
}
