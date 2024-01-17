package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.function.Function;

public interface MeshAppender {
    /**
     * Called to add appropriate geometry to the section.
     * @param context render context for this section
     */
    void render(Context context);

    /**
     * Section rendering context for a MeshAppender.
     * @param vertexConsumerProvider Provides access to {@link VertexConsumer}s for each render layer. Any vertices
     *                               pumped into these vertex consumers will be added to the chunk's final mesh
     * @param blockRenderView The chunk section being rendered. You should only retrieve blocks using this, not the
     *                        client world
     * @param sectionOrigin The origin of the section in the world
     * @param sodiumBuildBuffers Provides access to the Sodium/Embeddium vertex writing APIs. Intended mainly for internal
     *                           use
     */
    record Context(Function<RenderType, VertexConsumer> vertexConsumerProvider,
                   BlockAndTintGetter blockRenderView,
                   SectionPos sectionOrigin,
                   ChunkBuildBuffers sodiumBuildBuffers) {}
}
