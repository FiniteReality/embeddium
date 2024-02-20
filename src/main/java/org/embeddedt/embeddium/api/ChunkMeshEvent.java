package org.embeddedt.embeddium.api;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.jetbrains.annotations.ApiStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * This event is fired by Embeddium to allow mods to add their own custom geometry to chunks as they are being meshed.
 *
 * <p>In vanilla, mods often accomplish this by using a <a href="https://github.com/BluSunrize/ImmersiveEngineering/blob/1.18.2/src/main/java/blusunrize/immersiveengineering/mixin/coremods/client/RebuildTaskMixin.java">mixin</a></a>
 * into {@link net.minecraft.client.renderer.chunk.ChunkRenderDispatcher.RenderChunk.RebuildTask}, but that is hacky and does not
 * work with Embeddium as it doesn't use vanilla's chunk task classes.
 *
 * <p>This event is fired on the main thread, and provides the world being rendered and the section position. If you
 * wish to add additional static geometry for this section, you should register your {@link MeshAppender} inside the
 * event handler. Note that {@link MeshAppender#render(MeshAppender.Context)} will be called on a worker thread, and
 * therefore accessing the world is not safe inside the appender itself. Instead, use the provided {@link BlockAndTintGetter}.
 */
public class ChunkMeshEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<ChunkMeshEvent> BUS = new EventHandlerRegistrar<>();

    private final Level world;
    private final SectionPos sectionOrigin;
    private List<MeshAppender> meshAppenders = null;

    ChunkMeshEvent(Level world, SectionPos sectionOrigin) {
        this.world = world;
        this.sectionOrigin = sectionOrigin;
    }

    /**
     * Retrieves the current world (not cloned chunk section).
     */
    public Level getWorld() {
        return world;
    }

    public SectionPos getSectionOrigin() {
        return sectionOrigin;
    }

    /**
     * Register a mesh appender to be called when this section is meshed. The appender will be called on a worker thread,
     * so it must not attempt to access shared client data that is not thread-safe (e.g. the full client world).
     * @param appender the mesh appender to add
     */
    public void addMeshAppender(MeshAppender appender) {
        if (meshAppenders == null) {
            meshAppenders = new ArrayList<>();
        }
        meshAppenders.add(appender);
    }

    @ApiStatus.Internal
    public static List<MeshAppender> post(Level world, SectionPos origin) {
        ChunkMeshEvent event = new ChunkMeshEvent(world, origin);
        BUS.post(event);
        return Objects.requireNonNullElse(event.meshAppenders, Collections.emptyList());
    }
}
