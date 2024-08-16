package org.embeddedt.embeddium.impl.mixin.core.world.map;

import org.embeddedt.embeddium.impl.render.chunk.map.ChunkStatus;
import org.embeddedt.embeddium.impl.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundForgetLevelChunkPacket;
import net.minecraft.network.protocol.game.ClientboundLightUpdatePacketData;
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
            method = "applyLightData",
            at = @At("RETURN")
    )
    private void onLightDataReceived(int x, int z, ClientboundLightUpdatePacketData data, boolean invalidateSection, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.level)
                .onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }

    @Inject(method = "handleForgetLevelChunk", at = @At("RETURN"))
    private void onChunkUnloadPacket(ClientboundForgetLevelChunkPacket packet, CallbackInfo ci) {
        ChunkTrackerHolder.get(this.level)
                .onChunkStatusRemoved(packet.pos().x, packet.pos().z, ChunkStatus.FLAG_ALL);
    }
}
