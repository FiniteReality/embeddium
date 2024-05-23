package org.embeddedt.embeddium.impl.render.chunk.compile.buffers;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.joml.Vector3fc;

public interface ChunkModelVertexConsumer extends VertexConsumer, AutoCloseable {
    /**
     * Flushes the last vertex in the pipeline if needed. Must be called when rendering finishes.
     */
    void close();

    void embeddium$setOffset(Vector3fc offset);
}
