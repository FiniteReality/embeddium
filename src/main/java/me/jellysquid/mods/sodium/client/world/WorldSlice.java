package me.jellysquid.mods.sodium.client.world;

import me.jellysquid.mods.sodium.client.world.biome.*;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import me.jellysquid.mods.sodium.client.world.cloned.PackedIntegerArrayExtended;
import me.jellysquid.mods.sodium.client.world.cloned.palette.ClonedPalette;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.data.worldgen.biome.Biomes;
import net.minecraft.util.BitStorage;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;

import java.util.Arrays;

/**
 * Takes a slice of world state (block states, biome and light data arrays) and copies the data for use in off-thread
 * operations. This allows chunk build tasks to see a consistent snapshot of chunk data at the exact moment the task was
 * created.
 *
 * World slices are not safe to use from multiple threads at once, but the data they contain is safe from modification
 * by the main client thread.
 *
 * Object pooling should be used to avoid huge allocations as this class contains many large arrays.
 */
public class WorldSlice implements BlockAndTintGetter, BiomeManager.NoiseBiomeSource {
    // The number of blocks on each axis in a section.
    private static final int SECTION_BLOCK_LENGTH = 16;

    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH * SECTION_BLOCK_LENGTH;

    // The radius of blocks around the origin chunk that should be copied.
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;

    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = Mth.roundUp(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The number of sections on each axis of this slice.
    private static final int SECTION_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the lookup tables used for mapping values to coordinate int pairs. The lookup table size is always
    // a power of two so that multiplications can be replaced with simple bit shifts in hot code paths.
    private static final int TABLE_LENGTH = Mth.smallestEncompassingPowerOfTwo(SECTION_LENGTH);

    // The number of bits needed for each X/Y/Z component in a lookup table.
    private static final int TABLE_BITS = Integer.bitCount(TABLE_LENGTH - 1);

    // The array size for the section lookup table.
    private static final int SECTION_TABLE_ARRAY_SIZE = TABLE_LENGTH * TABLE_LENGTH * TABLE_LENGTH;

    // Fallback BlockState to use if none were available in the array
    private static final BlockState NULL_BLOCK_STATE = Blocks.AIR.defaultBlockState();

    // The world this slice has copied data from
    private final Level world;

    // Local Section->BlockState table.
    private final BlockState[][] blockStatesArrays;

    // Local section copies. Read-only.
    private ClonedChunkSection[] sections;

    // The accessor used for fetching biome data from the slice
    private final BiomeSlice biomeSlice;

    // The biome blend cache
    private final BiomeColorCache biomeColors;

    // The biome blend cache for custom ColorResolvers
    private final ColorResolverCache biomeColorsLegacy;

    // The starting point from which this slice captures blocks
    private int baseX, baseY, baseZ;

    // The chunk origin of this slice
    private SectionPos origin;

    // The volume that this slice contains
    private BoundingBox volume;

    public static ChunkRenderContext prepare(Level world, SectionPos origin, ClonedChunkSectionCache sectionCache) {
        LevelChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        LevelChunkSection section = chunk.getSections()[origin.getY()];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        if (LevelChunkSection.isEmpty(section)) {
            return null;
        }

        BoundingBox volume = new BoundingBox(origin.minBlockX() - NEIGHBOR_BLOCK_RADIUS,
                origin.minBlockY() - NEIGHBOR_BLOCK_RADIUS,
                origin.minBlockZ() - NEIGHBOR_BLOCK_RADIUS,
                origin.maxBlockX() + NEIGHBOR_BLOCK_RADIUS,
                origin.maxBlockY() + NEIGHBOR_BLOCK_RADIUS,
                origin.maxBlockZ() + NEIGHBOR_BLOCK_RADIUS);

        // The min/max bounds of the chunks copied by this slice
        final int minChunkX = origin.getX() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkY = origin.getY() - NEIGHBOR_CHUNK_RADIUS;
        final int minChunkZ = origin.getZ() - NEIGHBOR_CHUNK_RADIUS;

        final int maxChunkX = origin.getX() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkY = origin.getY() + NEIGHBOR_CHUNK_RADIUS;
        final int maxChunkZ = origin.getZ() + NEIGHBOR_CHUNK_RADIUS;

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    sections[getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ)] =
                            sectionCache.acquire(chunkX, chunkY, chunkZ);
                }
            }
        }

        return new ChunkRenderContext(origin, sections, volume);
    }

    public WorldSlice(Level world) {
        this.world = world;

        this.sections = new ClonedChunkSection[SECTION_TABLE_ARRAY_SIZE];
        this.blockStatesArrays = new BlockState[SECTION_TABLE_ARRAY_SIZE][];

        this.biomeSlice = new BiomeSlice();
        this.biomeColors = new BiomeColorCache(this.biomeSlice);
        this.biomeColorsLegacy = new ColorResolverCache(this.biomeSlice);

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int i = getLocalSectionIndex(x, y, z);

                    this.blockStatesArrays[i] = new BlockState[SECTION_BLOCK_COUNT];
                    Arrays.fill(this.blockStatesArrays[i], Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    public void copyData(ChunkRenderContext context) {
        this.origin = context.getOrigin();
        this.sections = context.getSections();
        this.volume = context.getVolume();

        this.baseX = (this.origin.getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.baseY = (this.origin.getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.baseZ = (this.origin.getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;

        for (int x = 0; x < SECTION_LENGTH; x++) {
            for (int y = 0; y < SECTION_LENGTH; y++) {
                for (int z = 0; z < SECTION_LENGTH; z++) {
                    int idx = getLocalSectionIndex(x, y, z);

                    this.unpackBlockData(this.blockStatesArrays[idx], this.sections[idx], context.getVolume());
                }
            }
        }

        this.biomeSlice.update((ClientLevel) this.world, context);
        this.biomeColors.update(context);
        this.biomeColorsLegacy.update(context);
    }

    private void unpackBlockData(BlockState[] states, ClonedChunkSection section, BoundingBox box) {
        if (this.origin.equals(section.getPosition()))  {
            this.unpackBlockDataZ(states, section);
        } else {
            this.unpackBlockDataR(states, section, box);
        }
    }

    private void unpackBlockDataR(BlockState[] states, ClonedChunkSection section, BoundingBox box) {
        BitStorage intArray = section.getBlockData();
        ClonedPalette<BlockState> palette = section.getBlockPalette();

        SectionPos pos = section.getPosition();

        int minBlockX = Math.max(box.x0, pos.minBlockX());
        int maxBlockX = Math.min(box.x1, pos.maxBlockX());

        int minBlockY = Math.max(box.y0, pos.minBlockY());
        int maxBlockY = Math.min(box.y1, pos.maxBlockY());

        int minBlockZ = Math.max(box.z0, pos.minBlockZ());
        int maxBlockZ = Math.min(box.z1, pos.maxBlockZ());

        for (int y = minBlockY; y <= maxBlockY; y++) {
            for (int z = minBlockZ; z <= maxBlockZ; z++) {
                for (int x = minBlockX; x <= maxBlockX; x++) {
                    int blockIdx = getLocalBlockIndex(x & 15, y & 15, z & 15);
                    int value = intArray.get(blockIdx);

                    states[blockIdx] = palette.get(value);
                }
            }
        }
    }

    private void unpackBlockDataZ(BlockState[] states, ClonedChunkSection section) {
        ((PackedIntegerArrayExtended) section.getBlockData())
                .copyUsingPalette(states, section.getBlockPalette());
    }

    private static boolean blockBoxContains(BoundingBox box, int x, int y, int z) {
        return x >= box.x0 &&
                x <= box.x1 &&
                y >= box.y0 &&
                y <= box.y1 &&
                z >= box.z0 &&
                z <= box.z1;
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        if (!blockBoxContains(this.volume, x, y, z)) {
            return Blocks.AIR.defaultBlockState();
        }

        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;

        return this.blockStatesArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                [getLocalBlockIndex(relX & 15, relY & 15, relZ & 15)];
    }

    public BlockState getBlockStateRelative(int x, int y, int z) {
        // NOTE: Not bounds checked. We assume ChunkRenderRebuildTask is the only function using this
        return this.blockStatesArrays[getLocalSectionIndex(x >> 4, y >> 4, z >> 4)]
                [getLocalBlockIndex(x & 15, y & 15, z & 15)];
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return this.getBlockState(pos)
                .getFluidState();
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return this.world.getShade(direction, shaded);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return this.world.getLightEngine();
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        if (!blockBoxContains(this.volume, x, y, z)) {
            return null;
        }

        int relX = x - this.baseX;
        int relY = y - this.baseY;
        int relZ = z - this.baseZ;

        return this.sections[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getBlockEntity(relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        BiomeColorSource source = BiomeColorSource.from(resolver);
        if(source != null)
            return this.biomeColors.getColor(source, pos.getX(), pos.getY(), pos.getZ());

        // fallback
        return this.biomeColorsLegacy.getColor(resolver, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        if (!blockBoxContains(this.volume, pos.getX(), pos.getY(), pos.getZ())) {
            return 0;
        }

        int relX = pos.getX() - this.baseX;
        int relY = pos.getY() - this.baseY;
        int relZ = pos.getZ() - this.baseZ;

        return this.sections[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                .getLightLevel(type, relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarkness) {
        return 0;
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return false;
    }

    @Override
    public Biome getNoiseBiome(int x, int y, int z) {
        int x2 = (x >> 2) - (this.baseX >> 4);
        int z2 = (z >> 2) - (this.baseZ >> 4);

        // Coordinates are in biome space!
        // [VanillaCopy] WorldView#getBiomeForNoiseGen(int, int, int)
        ClonedChunkSection section = this.sections[getLocalChunkIndex(x2, z2)];

        if (section != null ) {
            return section.getBiomeForNoiseGen(x, y, z);
        }

        return this.world.getUncachedNoiseBiome(x, y, z);
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    // [VanillaCopy] PalettedContainer#toIndex
    public static int getLocalBlockIndex(int x, int y, int z) {
        return y << 8 | z << 4 | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return y << TABLE_BITS << TABLE_BITS | z << TABLE_BITS | x;
    }

    public static int getLocalChunkIndex(int x, int z) {
        return z << TABLE_BITS | x;
    }
}
