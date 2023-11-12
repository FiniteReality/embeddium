package org.embeddedt.embeddium.api;

import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.Event;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ChunkMeshEvent extends Event {
    private final World world;
    private final ChunkSectionPos sectionOrigin;
    private List<MeshAppender> meshAppenders = null;

    ChunkMeshEvent(World world, ChunkSectionPos sectionOrigin) {
        this.world = world;
        this.sectionOrigin = sectionOrigin;
    }

    public World getWorld() {
        return world;
    }

    public ChunkSectionPos getSectionOrigin() {
        return sectionOrigin;
    }

    public void addMeshAppender(MeshAppender appender) {
        if (meshAppenders == null) {
            meshAppenders = new ArrayList<>();
        }
        meshAppenders.add(appender);
    }

    @ApiStatus.Internal
    public static List<MeshAppender> post(World world, ChunkSectionPos origin) {
        ChunkMeshEvent event = new ChunkMeshEvent(world, origin);
        MinecraftForge.EVENT_BUS.post(event);
        return Objects.requireNonNullElse(event.meshAppenders, Collections.emptyList());
    }
}
