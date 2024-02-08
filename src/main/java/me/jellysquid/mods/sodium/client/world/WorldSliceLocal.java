package me.jellysquid.mods.sodium.client.world;

import net.fabricmc.fabric.api.rendering.data.v1.RenderAttachedBlockView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipBlockStateContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Wrapper object used to defeat identity comparisons in mods. Since vanilla provides a unique object to them for each
 * subchunk, we do the same.
 */
public class WorldSliceLocal implements BlockAndTintGetter, RenderAttachedBlockView {
    private final BlockAndTintGetter view;

    public WorldSliceLocal(BlockAndTintGetter view) {
        this.view = view;
    }

    @Override
    public float getShade(Direction direction, boolean shaded) {
        return view.getShade(direction, shaded);
    }

    @Override
    public LevelLightEngine getLightEngine() {
        return view.getLightEngine();
    }

    @Override
    public int getBlockTint(BlockPos pos, ColorResolver colorResolver) {
        return view.getBlockTint(pos, colorResolver);
    }

    @Override
    public int getBrightness(LightLayer type, BlockPos pos) {
        return view.getBrightness(type, pos);
    }

    @Override
    public int getRawBrightness(BlockPos pos, int ambientDarkness) {
        return view.getRawBrightness(pos, ambientDarkness);
    }

    @Override
    public boolean canSeeSky(BlockPos pos) {
        return view.canSeeSky(pos);
    }

    @Override
    @Nullable
    public BlockEntity getBlockEntity(BlockPos pos) {
        return view.getBlockEntity(pos);
    }

    @Override
    public <T extends BlockEntity> Optional<T> getBlockEntity(BlockPos pos, BlockEntityType<T> type) {
        return view.getBlockEntity(pos, type);
    }

    @Override
    public BlockState getBlockState(BlockPos pos) {
        return view.getBlockState(pos);
    }

    @Override
    public FluidState getFluidState(BlockPos pos) {
        return view.getFluidState(pos);
    }

    @Override
    public int getLightEmission(BlockPos pos) {
        return view.getLightEmission(pos);
    }

    @Override
    public int getMaxLightLevel() {
        return view.getMaxLightLevel();
    }

    @Override
    public Stream<BlockState> getBlockStates(AABB box) {
        return view.getBlockStates(box);
    }

    @Override
    public BlockHitResult isBlockInLine(ClipBlockStateContext context) {
        return view.isBlockInLine(context);
    }

    @Override
    public BlockHitResult clip(ClipContext context) {
        return view.clip(context);
    }

    @Override
    @Nullable
    public BlockHitResult clipWithInteractionOverride(Vec3 start, Vec3 end, BlockPos pos, VoxelShape shape, BlockState state) {
        return view.clipWithInteractionOverride(start, end, pos, shape, state);
    }

    @Override
    public double getBlockFloorHeight(VoxelShape blockCollisionShape, Supplier<VoxelShape> belowBlockCollisionShapeGetter) {
        return view.getBlockFloorHeight(blockCollisionShape, belowBlockCollisionShapeGetter);
    }

    @Override
    public double getBlockFloorHeight(BlockPos pos) {
        return view.getBlockFloorHeight(pos);
    }

    public static <T, C> T traverseBlocks(Vec3 start, Vec3 end, C context, BiFunction<C, BlockPos, T> blockHitFactory, Function<C, T> missFactory) {
        return BlockGetter.traverseBlocks(start, end, context, blockHitFactory, missFactory);
    }

    @Override
    public int getHeight() {
        return view.getHeight();
    }

    @Override
    public int getMinBuildHeight() {
        return view.getMinBuildHeight();
    }

    @Override
    public int getMaxBuildHeight() {
        return view.getMaxBuildHeight();
    }

    @Override
    public int getSectionsCount() {
        return view.getSectionsCount();
    }

    @Override
    public int getMinSection() {
        return view.getMinSection();
    }

    @Override
    public int getMaxSection() {
        return view.getMaxSection();
    }

    @Override
    public boolean isOutsideBuildHeight(BlockPos pos) {
        return view.isOutsideBuildHeight(pos);
    }

    @Override
    public boolean isOutsideBuildHeight(int y) {
        return view.isOutsideBuildHeight(y);
    }

    @Override
    public int getSectionIndex(int y) {
        return view.getSectionIndex(y);
    }

    @Override
    public int getSectionIndexFromSectionY(int coord) {
        return view.getSectionIndexFromSectionY(coord);
    }

    @Override
    public int getSectionYFromSectionIndex(int index) {
        return view.getSectionYFromSectionIndex(index);
    }

    @Override
    public @Nullable Object getBlockEntityRenderData(BlockPos pos) {
        return view.getBlockEntityRenderData(pos);
    }

    @Override
    public boolean hasBiomes() {
        return view.hasBiomes();
    }

    @Override
    public Holder<Biome> getBiomeFabric(BlockPos pos) {
        return view.getBiomeFabric(pos);
    }

    public static LevelHeightAccessor create(int bottomY, int height) {
        return LevelHeightAccessor.create(bottomY, height);
    }
}
