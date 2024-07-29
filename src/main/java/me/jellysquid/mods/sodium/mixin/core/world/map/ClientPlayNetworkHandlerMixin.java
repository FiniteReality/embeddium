package me.jellysquid.mods.sodium.mixin.core.world.map;

import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public class ClientPlayNetworkHandlerMixin {
    @Shadow
    private ClientLevel level;

    @Inject(
            method = "handleLightUpdatePacked",
            at = @At("RETURN")
    )
    private void onLightDataReceived(ClientboundLightUpdatePacket data, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.level)
                .onChunkStatusAdded(data.getX(), data.getZ(), ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void onChunkUnloadPacket(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.level)
                .onChunkStatusRemoved(packet.getX(), packet.getZ(), ChunkStatus.FLAG_ALL);
    }
}
