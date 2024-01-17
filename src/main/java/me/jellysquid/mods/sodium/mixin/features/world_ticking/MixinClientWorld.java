package me.jellysquid.mods.sodium.mixin.features.world_ticking;

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
import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public abstract class MixinClientWorld extends Level {
    @Shadow
    protected abstract void trySpawnDripParticles(BlockPos pos, BlockState state, ParticleOptions parameters, boolean bl);

    protected MixinClientWorld(WritableLevelData mutableWorldProperties, ResourceKey<Level> registryKey,
                               DimensionType dimensionType, Supplier<ProfilerFiller> profiler, boolean bl, boolean bl2, long l) {
        super(mutableWorldProperties, registryKey, dimensionType, profiler, bl, bl2, l);
    }

    @Redirect(method = "animateTick", at = @At(value = "NEW", target = "java/util/Random"))
    private Random redirectRandomTickRandom() {
        return new XoRoShiRoRandom();
    }

    /**
     * @reason Avoid allocations, branch code out, early-skip some code
     * @author JellySquid
     */
    @Overwrite
    public void doAnimateTick(int xCenter, int yCenter, int zCenter, int radius, Random random, boolean spawnBarrierParticles, BlockPos.MutableBlockPos pos) {
        int x = xCenter + (random.nextInt(radius) - random.nextInt(radius));
        int y = yCenter + (random.nextInt(radius) - random.nextInt(radius));
        int z = zCenter + (random.nextInt(radius) - random.nextInt(radius));

        pos.set(x, y, z);

        BlockState blockState = this.getBlockState(pos);

        if (!blockState.isAir()) {
            this.performBlockDisplayTick(blockState, pos, random, spawnBarrierParticles);
        }

        if (!blockState.isCollisionShapeFullBlock(this, pos)) {
            this.performBiomeParticleDisplayTick(pos, random);
        }

        FluidState fluidState = blockState.getFluidState();

        if (!fluidState.isEmpty()) {
            this.performFluidDisplayTick(blockState, fluidState, pos, random);
        }
    }

    private void performBlockDisplayTick(BlockState blockState, BlockPos pos, Random random, boolean spawnBarrierParticles) {
        blockState.getBlock().animateTick(blockState, this, pos, random);

        if (spawnBarrierParticles && blockState.is(Blocks.BARRIER)) {
            this.performBarrierDisplayTick(pos);
        }
    }

    private void performBarrierDisplayTick(BlockPos pos) {
        this.addParticle(ParticleTypes.BARRIER, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D,
                0.0D, 0.0D, 0.0D);
    }

    private void performBiomeParticleDisplayTick(BlockPos pos, Random random) {
        AmbientParticleSettings config = this.getBiome(pos)
                .getAmbientParticle()
                .orElse(null);

        if (config != null && config.canSpawn(random)) {
            this.addParticle(config.getOptions(),
                    pos.getX() + random.nextDouble(),
                    pos.getY() + random.nextDouble(),
                    pos.getZ() + random.nextDouble(),
                    0.0D, 0.0D, 0.0D);
        }
    }

    private void performFluidDisplayTick(BlockState blockState, FluidState fluidState, BlockPos pos, Random random) {
        fluidState.animateTick(this, pos, random);

        ParticleOptions particleEffect = fluidState.getDripParticle();

        if (particleEffect != null && random.nextInt(10) == 0) {
            boolean solid = blockState.isFaceSturdy(this, pos, Direction.DOWN);

            // FIXME: don't allocate here
            BlockPos blockPos = pos.below();
            this.trySpawnDripParticles(blockPos, this.getBlockState(blockPos), particleEffect, solid);
        }
    }
}
