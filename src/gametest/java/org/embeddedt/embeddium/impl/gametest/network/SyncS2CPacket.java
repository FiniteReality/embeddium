package org.embeddedt.embeddium.impl.gametest.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.gametest.content.TestRegistry;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class SyncS2CPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<SyncS2CPacket> TYPE = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath(EmbeddiumConstants.MODID, "test_barrier"));
    public static final StreamCodec<ByteBuf, SyncS2CPacket> STREAM_CODEC = UUIDUtil.STREAM_CODEC.map(SyncS2CPacket::new, p -> p.uuid);

    private final UUID uuid;

    private static final ConcurrentHashMap<UUID, CountDownLatch> LATCHES = new ConcurrentHashMap<>();

    public SyncS2CPacket() {
        this.uuid = UUID.randomUUID();
    }

    public SyncS2CPacket(UUID uuid) {
        this.uuid = uuid;
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void applyBarrier() {
        var latch = new CountDownLatch(1);
        CountDownLatch prevLatch = LATCHES.put(this.uuid, latch);
        if(prevLatch != null) {
            throw new IllegalStateException("Existing latch found for barrier UUID: " + this.uuid);
        }
        TestRegistry.LOGGER.debug("Waiting at barrier {}", this.uuid);
        var player = Minecraft.getInstance().getSingleplayerServer().getPlayerList().getPlayers().getFirst();
        // 1.21 - need to enable flushing so packets reach the client synchronously
        player.connection.resumeFlushing();
        PacketDistributor.sendToAllPlayers(this);
        player.connection.suspendFlushing();
        try {
            if(!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Client did not reach barrier " + this.uuid);
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        TestRegistry.LOGGER.debug("Continuing past barrier {}", this.uuid);
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            var latch = LATCHES.remove(this.uuid);
            if(latch == null) {
                throw new IllegalStateException("Latch for " + this.uuid + " not found");
            } else {
                TestRegistry.LOGGER.debug("Reached barrier {}", this.uuid);
                latch.countDown();
            }
        });
    }
}
