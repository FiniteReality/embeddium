package org.embeddedt.embeddium.gl.buffer;

import org.embeddedt.embeddium.gl.attribute.GlVertexFormat;
import org.embeddedt.embeddium.util.NativeBuffer;

/**
 * Helper type for tagging the vertex format alongside the raw buffer data.
 */
public record IndexedVertexData(GlVertexFormat<?> vertexFormat,
                                NativeBuffer vertexBuffer,
                                NativeBuffer indexBuffer) {
    public void delete() {
        this.vertexBuffer.free();
        this.indexBuffer.free();
    }
}
