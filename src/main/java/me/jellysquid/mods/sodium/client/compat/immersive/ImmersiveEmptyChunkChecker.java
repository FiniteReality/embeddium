package me.jellysquid.mods.sodium.client.compat.immersive;

import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireCollisionData.ConnectionSegments;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.List;

public class ImmersiveEmptyChunkChecker {
    public static boolean hasWires(ChunkSectionPos origin) {
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(MinecraftClient.getInstance().world);
        List<ConnectionSegments> wiresInSection = globalNet.getCollisionData().getWiresIn(origin);
        return wiresInSection != null && !wiresInSection.isEmpty();
    }
}