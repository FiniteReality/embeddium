package me.jellysquid.mods.sodium.client.world;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.lighting.LevelLightEngine;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.VoxelShape;
import javax.annotation.Nullable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Wrapper object used to defeat identity comparisons in mods. Since vanilla provides a unique object to them for each
 * subchunk, we do the same.
 */
public class WorldSliceLocal implements BlockAndTintGetter {
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
    public int getMaxBuildHeight() {
        return view.getMaxBuildHeight();
    }

    @Override
    public Stream<BlockState> getBlockStates(AABB arg) {
        return view.getBlockStates(arg);
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

    public static <T> T traverseBlocks(ClipContext arg, BiFunction<ClipContext, BlockPos, T> context, Function<ClipContext, T> blockRaycaster) {
        return BlockGetter.traverseBlocks(arg, context, blockRaycaster);
    }
}
