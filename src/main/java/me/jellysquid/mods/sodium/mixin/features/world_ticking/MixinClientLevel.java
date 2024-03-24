package me.jellysquid.mods.sodium.mixin.features.world_ticking;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.WritableLevelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.Supplier;

// Use a very low priority so most injects into doAnimateTick will still work
@Mixin(value = ClientLevel.class, priority = 500)
public abstract class MixinClientLevel extends Level {
    protected MixinClientLevel(WritableLevelData p_220352_, ResourceKey<Level> p_220353_, Holder<DimensionType> p_220354_, Supplier<ProfilerFiller> p_220355_, boolean p_220356_, boolean p_220357_, long p_220358_, int p_220359_) {
        super(p_220352_, p_220353_, p_220354_, p_220355_, p_220356_, p_220357_, p_220358_, p_220359_);
    }

    @Shadow
    private void lambda$doAnimateTick$8(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {
        throw new AssertionError();
    }

    @Shadow
    protected abstract void trySpawnDripParticles(BlockPos p_104690_, BlockState p_104691_, ParticleOptions p_104692_, boolean p_104693_);

    /**
     * @author embeddedt
     * @reason Use singlethreaded random to avoid AtomicLong overhead
     */
    @Redirect(method = "animateTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/RandomSource;create()Lnet/minecraft/util/RandomSource;"))
    private RandomSource createLocal() {
        return new SingleThreadedRandomSource(RandomSupport.generateUniqueSeed());
    }

    /**
     * @author embeddedt
     * @reason Avoid allocations & do some misc optimizations. Partially based on old Sodium 0.2 mixin
     */
    @Overwrite
    public void doAnimateTick(int xCenter, int yCenter, int zCenter, int radius, RandomSource random, @Nullable Block markerBlock, BlockPos.MutableBlockPos pos) {
        int x = xCenter + (random.nextInt(radius) - random.nextInt(radius));
        int y = yCenter + (random.nextInt(radius) - random.nextInt(radius));
        int z = zCenter + (random.nextInt(radius) - random.nextInt(radius));

        pos.set(x, y, z);

        BlockState blockState = this.getBlockState(pos);

        if (!blockState.isAir()) {
            blockState.getBlock().animateTick(blockState, this, pos, random);
        }

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

        if (blockState.getBlock() == markerBlock) {
            this.addParticle(new BlockParticleOption(ParticleTypes.BLOCK_MARKER, blockState), (double)xCenter + 0.5D, (double)yCenter + 0.5D, (double)zCenter + 0.5D, 0.0D, 0.0D, 0.0D);
        }

        if (!blockState.isCollisionShapeFullBlock(this, pos)) {
            var particleOpt = this.getBiome(pos).value().getAmbientParticle();

            //noinspection OptionalIsPresent
            if(particleOpt.isPresent()) {
                lambda$doAnimateTick$8(pos, particleOpt.get());
            }
        }
    }
}
