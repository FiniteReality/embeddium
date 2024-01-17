package org.embeddedt.embeddium.compat.immersive;

import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireCollisionData.ConnectionSegments;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.core.SectionPos;

public class ImmersiveEmptyChunkChecker {
    public static boolean hasWires(SectionPos origin) {
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(Minecraft.getInstance().level);
        List<ConnectionSegments> wiresInSection = globalNet.getCollisionData().getWiresIn(origin);
        return wiresInSection != null && !wiresInSection.isEmpty();
    }
}