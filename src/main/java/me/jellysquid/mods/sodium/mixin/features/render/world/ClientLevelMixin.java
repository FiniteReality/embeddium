package me.jellysquid.mods.sodium.mixin.features.render.world;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.AmbientParticleSettings;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Optional;
import java.util.function.Consumer;

@Mixin(ClientLevel.class)
public class ClientLevelMixin {
    @Shadow
    private void method_24462(BlockPos.MutableBlockPos pos, AmbientParticleSettings settings) {
        throw new AssertionError();
    }

    /**
     * @author embeddedt
     * @reason Avoid allocating a capturing lambda for each ticked block position. The original allocated method arg
     * is discarded by the JIT.
     */
    @Redirect(method = "doAnimateTick", at = @At(value = "INVOKE", target = "Ljava/util/Optional;ifPresent(Ljava/util/function/Consumer;)V"))
    private void addBiomeParticleWithoutAlloc(Optional<AmbientParticleSettings> particleSettings, Consumer<AmbientParticleSettings> allocatedLambda, int p_233613_, int p_233614_, int p_233615_, int p_233616_, RandomSource p_233617_, Block p_233618_, BlockPos.MutableBlockPos p_233619_) {
        //noinspection OptionalIsPresent
        if(particleSettings.isPresent()) {
            method_24462(p_233619_, particleSettings.get());
        }
    }
}
