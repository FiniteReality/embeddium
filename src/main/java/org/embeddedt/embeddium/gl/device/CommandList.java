package org.embeddedt.embeddium.gl.device;

import org.embeddedt.embeddium.gl.array.GlVertexArray;
import org.embeddedt.embeddium.gl.buffer.*;
import org.embeddedt.embeddium.gl.sync.GlFence;
import org.embeddedt.embeddium.gl.tessellation.GlPrimitiveType;
import org.embeddedt.embeddium.gl.tessellation.GlTessellation;
import org.embeddedt.embeddium.gl.tessellation.TessellationBinding;
import org.embeddedt.embeddium.gl.util.EnumBitField;

import java.nio.ByteBuffer;

public interface CommandList extends AutoCloseable {
    GlMutableBuffer createMutableBuffer();

    GlImmutableBuffer createImmutableBuffer(long bufferSize, EnumBitField<GlBufferStorageFlags> flags);

    GlTessellation createTessellation(GlPrimitiveType primitiveType, TessellationBinding[] bindings);

    void bindVertexArray(GlVertexArray array);

    void uploadData(GlMutableBuffer glBuffer, ByteBuffer byteBuffer, GlBufferUsage usage);

    void copyBufferSubData(GlBuffer src, GlBuffer dst, long readOffset, long writeOffset, long bytes);

    void bindBuffer(GlBufferTarget target, GlBuffer buffer);

    void unbindVertexArray();

    void allocateStorage(GlMutableBuffer buffer, long bufferSize, GlBufferUsage usage);

    void deleteBuffer(GlBuffer buffer);

    void deleteVertexArray(GlVertexArray vertexArray);

    void flush();

    DrawCommandList beginTessellating(GlTessellation tessellation);

    void deleteTessellation(GlTessellation tessellation);

    @Override
    default void close() {
        this.flush();
    }

    GlBufferMapping mapBuffer(GlBuffer buffer, long offset, long length, EnumBitField<GlBufferMapFlags> flags);

    void unmap(GlBufferMapping map);

    void flushMappedRange(GlBufferMapping map, int offset, int length);

    GlFence createFence();
}
