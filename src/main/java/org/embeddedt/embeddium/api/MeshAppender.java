package org.embeddedt.embeddium.api;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.world.BlockRenderView;

import java.util.function.Function;

public interface MeshAppender {
    void render(Context context);

    record Context(Function<RenderLayer, VertexConsumer> vertexConsumerProvider,
                   BlockRenderView blockRenderView,
                   ChunkSectionPos sectionOrigin,
                   ChunkBuildBuffers sodiumBuildBuffers) {}
}
