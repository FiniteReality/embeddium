package me.jellysquid.mods.sodium.mixin.features.render.world;

import me.jellysquid.mods.sodium.client.util.rand.XoRoShiRoRandom;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Random;
import java.util.function.Consumer;
import java.util.function.Supplier;

// Use a very low priority so most injects into doAnimateTick will still work
@Mixin(value = ClientLevel.class, priority = 500)
public abstract class ClientLevelMixin extends Level {
    protected ClientLevelMixin(WritableLevelData levelData, ResourceKey<Level> dimension, DimensionType dimensionType, Supplier<ProfilerFiller> profiler, boolean isClientSide, boolean isDebug, long biomeZoomSeed) {
        super(levelData, dimension, dimensionType, profiler, isClientSide, isDebug, biomeZoomSeed);
    }

    @Shadow
    private void lambda$doAnimateTick$4(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {
        throw new AssertionError();
    }

    private BlockPos.MutableBlockPos embeddium$particlePos;
    private final Consumer<AmbientParticleSettings> embeddium$particleSettingsConsumer = settings -> lambda$doAnimateTick$4(embeddium$particlePos, settings);

    @Shadow
    protected abstract void trySpawnDripParticles(BlockPos p_104690_, BlockState p_104691_, ParticleOptions p_104692_, boolean p_104693_);

    /**
     * @author embeddedt
     * @reason Use singlethreaded random to avoid AtomicLong overhead
     */
    @Redirect(method = "animateTick", at = @At(value = "NEW", target = "()Ljava/util/Random;"))
    private Random createLocal() {
        return new XoRoShiRoRandom();
    }

    /**
     * @author embeddedt
     * @reason Avoid allocations & do some misc optimizations. Partially based on old Sodium 0.2 mixin
     */
    @Overwrite
    public void doAnimateTick(int xCenter, int yCenter, int zCenter, int radius, Random random, boolean drawBarriers, BlockPos.MutableBlockPos pos) {
        int x = xCenter + (random.nextInt(radius) - random.nextInt(radius));
        int y = yCenter + (random.nextInt(radius) - random.nextInt(radius));
        int z = zCenter + (random.nextInt(radius) - random.nextInt(radius));

        pos.set(x, y, z);

        BlockState blockState = this.getBlockState(pos);

        blockState.getBlock().animateTick(blockState, this, pos, random);

        FluidState fluidState = blockState.getFluidState();

        if (!fluidState.isEmpty()) {
            fluidState.animateTick(this, pos, random);
            ParticleOptions particleoptions = fluidState.getDripParticle();
            if (particleoptions != null && random.nextInt(10) == 0) {
                boolean flag = blockState.isFaceSturdy(this, pos, Direction.DOWN);
                BlockPos blockpos = pos.below();
                this.trySpawnDripParticles(blockpos, this.getBlockState(blockpos), particleoptions, flag);
            }
        }

        if (drawBarriers && blockState.getBlock() == Blocks.BARRIER) {
            this.addParticle(ParticleTypes.BARRIER, (double)x + 0.5D, (double)y + 0.5D, (double)z + 0.5D, 0.0D, 0.0D, 0.0D);
        }

        if (!blockState.isCollisionShapeFullBlock(this, pos)) {
            // This dance looks ridiculous over just calling the lambda, but it's needed because mod mixins target the ifPresent call.
            // The important part (skipping the allocation) still happens.
            embeddium$particlePos = pos;
            this.getBiome(pos).getAmbientParticle().ifPresent(embeddium$particleSettingsConsumer);
        }
    }
}
