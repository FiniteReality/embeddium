package me.jellysquid.mods.sodium.client.render.chunk.data;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionFlags;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.VisibilityEncoding;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.embeddedt.embeddium.api.render.chunk.SectionInfoBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.IntFunction;
import java.util.function.Predicate;

/**
 * The render data for a chunk render container containing all the information about which meshes are attached, the
 * block entities contained by it, and any data used for occlusion testing.
 */
public class BuiltSectionInfo {
    public static final BuiltSectionInfo EMPTY = createEmptyData();

    public final int flags;
    public final long visibilityData;

    public final BlockEntity @Nullable[] globalBlockEntities;
    public final BlockEntity @Nullable[] culledBlockEntities;
    public final TextureAtlasSprite @Nullable[] animatedSprites;

    private BuiltSectionInfo(@NotNull Collection<TerrainRenderPass> blockRenderPasses,
                             @NotNull Collection<BlockEntity> globalBlockEntities,
                             @NotNull Collection<BlockEntity> culledBlockEntities,
                             @NotNull Collection<TextureAtlasSprite> animatedSprites,
                             @NotNull VisibilitySet occlusionData) {
        this.globalBlockEntities = toArray(globalBlockEntities, BlockEntity[]::new);
        this.culledBlockEntities = toArray(culledBlockEntities, BlockEntity[]::new);
        this.animatedSprites = toArray(animatedSprites, TextureAtlasSprite[]::new);

        int flags = 0;

        if (!blockRenderPasses.isEmpty()) {
            flags |= 1 << RenderSectionFlags.HAS_BLOCK_GEOMETRY;
            if(blockRenderPasses.contains(DefaultTerrainRenderPasses.TRANSLUCENT))
                flags |= 1 << RenderSectionFlags.HAS_TRANSLUCENT_DATA;
        }

        if (!culledBlockEntities.isEmpty()) {
            flags |= 1 << RenderSectionFlags.HAS_BLOCK_ENTITIES;
        }

        if (!animatedSprites.isEmpty()) {
            flags |= 1 << RenderSectionFlags.HAS_ANIMATED_SPRITES;
        }

        this.flags = flags;

        this.visibilityData = VisibilityEncoding.encode(occlusionData);
    }

    public static class Builder implements SectionInfoBuilder {
        private final List<TerrainRenderPass> blockRenderPasses = new ArrayList<>();
        private final List<BlockEntity> globalBlockEntities = new ArrayList<>();
        private final List<BlockEntity> culledBlockEntities = new ArrayList<>();
        private final Set<TextureAtlasSprite> animatedSprites = new ObjectOpenHashSet<>();

        private VisibilitySet occlusionData;

        public void addRenderPass(TerrainRenderPass pass) {
            this.blockRenderPasses.add(pass);
        }

        public void setOcclusionData(VisibilitySet data) {
            this.occlusionData = data;
        }

        @Override
        public void addSprite(TextureAtlasSprite sprite) {
            if (SpriteUtil.hasAnimation(sprite)) {
                this.animatedSprites.add(sprite);
            }
        }

        @Override
        public void addBlockEntity(BlockEntity entity, boolean cull) {
            (cull ? this.culledBlockEntities : this.globalBlockEntities).add(entity);
        }

        @Override
        public void removeBlockEntitiesIf(Predicate<BlockEntity> filter) {
            this.culledBlockEntities.removeIf(filter);
            this.globalBlockEntities.removeIf(filter);
        }

        public BuiltSectionInfo build() {
            return new BuiltSectionInfo(this.blockRenderPasses, this.globalBlockEntities, this.culledBlockEntities, this.animatedSprites, this.occlusionData);
        }
    }

    private static BuiltSectionInfo createEmptyData() {
        VisibilitySet occlusionData = new VisibilitySet();
        occlusionData.add(EnumSet.allOf(Direction.class));

        BuiltSectionInfo.Builder meshInfo = new BuiltSectionInfo.Builder();
        meshInfo.setOcclusionData(occlusionData);

        return meshInfo.build();
    }

    private static <T> T[] toArray(Collection<T> collection, IntFunction<T[]> allocator) {
        if (collection.isEmpty()) {
            return null;
        }

        return collection.toArray(allocator);
    }
}