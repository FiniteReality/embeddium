package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import java.util.function.Supplier;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.world.BiomeSeedProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientLevel.class)
public class MixinClientWorld implements BiomeSeedProvider
{
    @Unique
    private long biomeSeed;

    @Inject(method = "setLightReady", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/chunk/LevelChunk;setClientLightReady(Z)V", shift = At.Shift.AFTER))
    private void postLightUpdate(int chunkX, int chunkZ, CallbackInfo ci) {
        SodiumWorldRenderer.instance()
                .onChunkLightAdded(chunkX, chunkZ);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(ClientPacketListener netHandler, ClientLevel.ClientLevelData properties, ResourceKey<Level> registryRef, Holder registryEntry, int loadDistance, int simulationDistance, Supplier profiler, LevelRenderer worldRenderer, boolean debugWorld, long seed, CallbackInfo ci) {
        this.biomeSeed = seed;
    }

    @Override
    public long getBiomeSeed() {
        return this.biomeSeed;
    }
}
