package me.jellysquid.mods.sodium.client.world;

import it.unimi.dsi.fastutil.ints.Int2ReferenceMap;
import me.jellysquid.mods.sodium.client.world.biome.*;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.DataLayer;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import org.embeddedt.embeddium.api.ChunkMeshEvent;
import org.embeddedt.embeddium.api.MeshAppender;
import org.embeddedt.embeddium.asm.OptionalInterface;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * <p>Takes a slice of world state (block states, biome and light data arrays) and copies the data for use in off-thread
 * operations. This allows chunk build tasks to see a consistent snapshot of chunk data at the exact moment the task was
 * created.</p>
 *
 * <p>World slices are not safe to use from multiple threads at once, but the data they contain is safe from modification
 * by the main client thread.</p>
 *
 * <p>Object pooling should be used to avoid huge allocations as this class contains many large arrays.</p>
 */
@OptionalInterface({ RenderAttachedBlockView.class })
public class WorldSlice implements BlockAndTintGetter, BiomeColorView, RenderAttachedBlockView {
    private static final LightLayer[] LIGHT_TYPES = LightLayer.values();

    // The number of blocks in a section.
    private static final int SECTION_BLOCK_COUNT = 16 * 16 * 16;

    // The radius of blocks around the origin chunk that should be copied.
    private static final int NEIGHBOR_BLOCK_RADIUS = 2;

    // The radius of chunks around the origin chunk that should be copied.
    private static final int NEIGHBOR_CHUNK_RADIUS = 1; // TODO //Mth.roundToward(NEIGHBOR_BLOCK_RADIUS, 16) >> 4;

    // The number of sections on each axis of this slice.
    private static final int SECTION_ARRAY_LENGTH = 1 + (NEIGHBOR_CHUNK_RADIUS * 2);

    // The size of the (Local Section -> Resource) arrays.
    private static final int SECTION_ARRAY_SIZE = SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH;

    // The number of blocks on each axis of this slice.
    private static final int BLOCK_ARRAY_LENGTH = SECTION_ARRAY_LENGTH * 16;

    // The number of bits needed for each local X/Y/Z coordinate.
    private static final int LOCAL_XYZ_BITS = 4;

    // The default block state used for out-of-bounds access
    private static final BlockState EMPTY_BLOCK_STATE = Blocks.AIR.defaultBlockState();

    // The world this slice has copied data from
    public final ClientLevel world;

    // The accessor used for fetching biome data from the slice
    private final BiomeSlice biomeSlice;

    // The biome blend cache
    private final BiomeColorCache biomeColors;

    // (Local Section -> Block States) table.
    private final BlockState[][] blockArrays;

    // (Local Section -> Light Arrays) table.
    private final @Nullable DataLayer[][] lightArrays;

    // (Local Section -> Block Entity) table.
    private final @Nullable Int2ReferenceMap<BlockEntity>[] blockEntityArrays;

    // (Local Section -> Block Entity Render Data) table.
    private final @Nullable Int2ReferenceMap<Object>[] blockEntityRenderDataArrays;

    // The starting point from which this slice captures blocks
    private int originX, originY, originZ;
    
    // The volume that this WorldSlice contains
    private BoundingBox volume;

    public static ChunkRenderContext prepare(Level world, SectionPos origin, ClonedChunkSectionCache sectionCache) {
        LevelChunk chunk = world.getChunk(origin.getX(), origin.getZ());
        LevelChunkSection section = chunk.getSections()[origin.getY() / 16];

        // If the chunk section is absent or empty, simply terminate now. There will never be anything in this chunk
        // section to render, so we need to signal that a chunk render task shouldn't created. This saves a considerable
        // amount of time in queueing instant build tasks and greatly accelerates how quickly the world can be loaded.
        List<MeshAppender> meshAppenders = ChunkMeshEvent.post(world, origin);
        boolean isEmpty = LevelChunkSection.isEmpty(section) && meshAppenders.isEmpty();

        if (isEmpty) {
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

        ClonedChunkSection[] sections = new ClonedChunkSection[SECTION_ARRAY_SIZE];

        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                for (int chunkY = minChunkY; chunkY <= maxChunkY; chunkY++) {
                    sections[getLocalSectionIndex(chunkX - minChunkX, chunkY - minChunkY, chunkZ - minChunkZ)] =
                            sectionCache.acquire(chunkX, chunkY, chunkZ);
                }
            }
        }

        return new ChunkRenderContext(origin, sections, volume).withMeshAppenders(meshAppenders);
    }

    @SuppressWarnings("unchecked")
    public WorldSlice(ClientLevel world) {
        this.world = world;

        this.blockArrays = new BlockState[SECTION_ARRAY_SIZE][SECTION_BLOCK_COUNT];
        this.lightArrays = new DataLayer[SECTION_ARRAY_SIZE][LIGHT_TYPES.length];

        this.blockEntityArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];
        this.blockEntityRenderDataArrays = new Int2ReferenceMap[SECTION_ARRAY_SIZE];

        this.biomeSlice = new BiomeSlice();
        this.biomeColors = new BiomeColorCache(this.biomeSlice, Minecraft.getInstance().options.biomeBlendRadius);

        for (BlockState[] blockArray : this.blockArrays) {
            Arrays.fill(blockArray, EMPTY_BLOCK_STATE);
        }
    }

    public void copyData(ChunkRenderContext context) {
        this.originX = (context.getOrigin().getX() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.originY = (context.getOrigin().getY() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.originZ = (context.getOrigin().getZ() - NEIGHBOR_CHUNK_RADIUS) << 4;
        this.volume = context.getVolume();

        for (int x = 0; x < SECTION_ARRAY_LENGTH; x++) {
            for (int y = 0; y < SECTION_ARRAY_LENGTH; y++) {
                for (int z = 0; z < SECTION_ARRAY_LENGTH; z++) {
                    this.copySectionData(context, getLocalSectionIndex(x, y, z));
                }
            }
        }

        this.biomeSlice.update(this.world, context);
        this.biomeColors.update(context);
    }

    private void copySectionData(ChunkRenderContext context, int sectionIndex) {
        var section = context.getSections()[sectionIndex];

        Objects.requireNonNull(section, "Chunk section must be non-null");

        try {
            this.unpackBlockData(this.blockArrays[sectionIndex], context, section);
        } catch(RuntimeException e) {
            throw new IllegalStateException("Exception copying block data for section: " + section.getPosition(), e);
        }

        this.lightArrays[sectionIndex][LightLayer.BLOCK.ordinal()] = section.getLightArray(LightLayer.BLOCK);
        this.lightArrays[sectionIndex][LightLayer.SKY.ordinal()] = section.getLightArray(LightLayer.SKY);

        this.blockEntityArrays[sectionIndex] = section.getBlockEntityMap();
        this.blockEntityRenderDataArrays[sectionIndex] = section.getBlockEntityRenderDataMap();
    }

    private void unpackBlockData(BlockState[] blockArray, ChunkRenderContext context, ClonedChunkSection section) {
        if (section.getBlockData() == null) {
            Arrays.fill(blockArray, EMPTY_BLOCK_STATE);
            return;
        }

        var container = ReadableContainerExtended.of(section.getBlockData());

        SectionPos origin = context.getOrigin();
        SectionPos pos = section.getPosition();

        if (origin.equals(pos))  {
            container.sodium$unpack(blockArray);
        } else {
            var bounds = context.getVolume();

            int minBlockX = Math.max(bounds.x0, pos.minBlockX());
            int maxBlockX = Math.min(bounds.x1, pos.maxBlockX());

            int minBlockY = Math.max(bounds.y0, pos.minBlockY());
            int maxBlockY = Math.min(bounds.y1, pos.maxBlockY());

            int minBlockZ = Math.max(bounds.z0, pos.minBlockZ());
            int maxBlockZ = Math.min(bounds.z1, pos.maxBlockZ());

            container.sodium$unpack(blockArray, minBlockX & 15, minBlockY & 15, minBlockZ & 15,
                    maxBlockX & 15, maxBlockY & 15, maxBlockZ & 15);
        }
    }

    public void reset() {
        // erase any pointers to resources we no longer need
        // no point in cleaning the pre-allocated arrays (such as block state storage) since we hold the
        // only reference.
        for (int sectionIndex = 0; sectionIndex < SECTION_ARRAY_LENGTH; sectionIndex++) {
            Arrays.fill(this.lightArrays[sectionIndex], null);

            this.blockEntityArrays[sectionIndex] = null;
        }
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return this.getBlockState(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockState getBlockState(int x, int y, int z) {
        int relX = x - this.originX;
        int relY = y - this.originY;
        int relZ = z - this.originZ;

        if (!isInside(relX, relY, relZ)) {
            return EMPTY_BLOCK_STATE;
        }

        return this.blockArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)]
                [getLocalBlockIndex(relX & 15, relY & 15, relZ & 15)];
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
        // Not thread-safe to access lighting data from off-thread, even if Minecraft allows it.
        throw new UnsupportedOperationException();
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        if (!isInside(relX, relY, relZ)) {
            return 0;
        }

        var lightArray = this.lightArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)][type.ordinal()];

        if (lightArray == null) {
            // If the array is null, it means the dimension for the current world does not support that light type
            return 0;
        }

        return lightArray.get(relX & 15, relY & 15, relZ & 15);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarkness) {
        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        if (!isInside(relX, relY, relZ)) {
            return 0;
        }

        var lightArrays = this.lightArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        var skyLightArray = lightArrays[LightLayer.SKY.ordinal()];
        var blockLightArray = lightArrays[LightLayer.BLOCK.ordinal()];

        int localX = relX & 15;
        int localY = relY & 15;
        int localZ = relZ & 15;

        int skyLight = skyLightArray == null ? 0 : skyLightArray.get(localX, localY, localZ) - ambientDarkness;
        int blockLight = blockLightArray == null ? 0 : blockLightArray.get(localX, localY, localZ);

        return Math.max(blockLight, skyLight);
    }

    @Override
    public BlockEntity getBlockEntity(BlockPos pos) {
        return this.getBlockEntity(pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockEntity getBlockEntity(int x, int y, int z) {
        int relX = x - this.originX;
        int relY = y - this.originY;
        int relZ = z - this.originZ;

        if (!isInside(relX, relY, relZ)) {
            return null;
        }

        var blockEntities = this.blockEntityArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        if (blockEntities == null) {
            return null;
        }

        return blockEntities.get(getLocalBlockIndex(relX & 15, relY & 15, relZ & 15));
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver resolver) {
        return this.biomeColors.getColor(resolver, pos.getX(), pos.getY(), pos.getZ());
    }

    @Override
    public int getColor(BiomeColorSource source, int x, int y, int z) {
        return this.biomeColors.getColor(source, x, y, z);
    }

    @Override
    public Object getBlockEntityRenderAttachment(BlockPos pos) {
        int relX = pos.getX() - this.originX;
        int relY = pos.getY() - this.originY;
        int relZ = pos.getZ() - this.originZ;

        if (!isInside(relX, relY, relZ)) {
            return null;
        }

        var blockEntityRenderDataMap = this.blockEntityRenderDataArrays[getLocalSectionIndex(relX >> 4, relY >> 4, relZ >> 4)];

        if (blockEntityRenderDataMap == null) {
            return null;
        }

        return blockEntityRenderDataMap.get(getLocalBlockIndex(relX & 15, relY & 15, relZ & 15));
    }

    public static int getLocalBlockIndex(int x, int y, int z) {
        return (y << LOCAL_XYZ_BITS << LOCAL_XYZ_BITS) | (z << LOCAL_XYZ_BITS) | x;
    }

    public static int getLocalSectionIndex(int x, int y, int z) {
        return (y * SECTION_ARRAY_LENGTH * SECTION_ARRAY_LENGTH) + (z * SECTION_ARRAY_LENGTH) + x;
    }

    private boolean isInside(int relX, int relY, int relZ) {
        return relX >= 0 && relX < BLOCK_ARRAY_LENGTH && relZ >= 0 && relZ < BLOCK_ARRAY_LENGTH && relY >= 0 && relY < BLOCK_ARRAY_LENGTH;
    }
}
