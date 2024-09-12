package org.embeddedt.embeddium.impl.mixin.core.world.biome;

import org.embeddedt.embeddium.impl.world.BiomeSeedProvider;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Supplier;

@Mixin(ClientLevel.class)
public class ClientWorldMixin implements BiomeSeedProvider {
    @Unique
    private long biomeSeed;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void captureSeed(ClientPacketListener clientPacketListener, ClientLevel.ClientLevelData clientLevelData, ResourceKey resourceKey, Holder holder, int i, int j, LevelRenderer levelRenderer, boolean bl, long seed, int k, CallbackInfo ci) {
        this.biomeSeed = seed;
    }

    @Override
    public long sodium$getBiomeSeed() {
        return this.biomeSeed;
    }
}