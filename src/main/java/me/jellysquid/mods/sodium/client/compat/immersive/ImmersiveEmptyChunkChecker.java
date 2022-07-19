package me.jellysquid.mods.sodium.client.compat.immersive;

import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireCollisionData.ConnectionSegments;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderBuildTask;
import me.jellysquid.mods.sodium.client.render.chunk.tasks.ChunkRenderRebuildTask;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSection;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.ChunkSectionPos;

import java.util.Arrays;
import java.util.List;

public class ImmersiveEmptyChunkChecker {
    public static boolean hasWires(ChunkSectionPos origin) {
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(MinecraftClient.getInstance().world);
        List<ConnectionSegments> wiresInSection = globalNet.getCollisionData().getWiresIn(origin);
        return wiresInSection != null && !wiresInSection.isEmpty();
    }

    public static ChunkRenderBuildTask makeEmptyRebuildTask(
            ClonedChunkSectionCache sectionCache, ChunkSectionPos origin, RenderSection render, int frame
    ) {
        var sections = new ClonedChunkSection[64];
        // TODO rethink this mess, and possibly call release?
        var centerSection = sectionCache.acquire(origin.getSectionX(), origin.getSectionY(), origin.getSectionZ());
        Arrays.fill(sections, centerSection);
        var sectionBB = new BlockBox(
                origin.getMinX() - 2, origin.getMinY() - 2, origin.getMinZ() - 2,
                origin.getMaxX() + 2, origin.getMaxY() + 2, origin.getMaxZ() + 2
        );
        var context = new ChunkRenderContext(origin, sections, sectionBB);
        return new ChunkRenderRebuildTask(render, context, frame);
    }
}