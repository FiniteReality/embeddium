package org.embeddedt.embeddium.gl.arena.staging;

import org.embeddedt.embeddium.gl.buffer.GlBuffer;
import org.embeddedt.embeddium.gl.device.CommandList;

import java.nio.ByteBuffer;

public interface StagingBuffer {
    void enqueueCopy(CommandList commandList, ByteBuffer data, GlBuffer dst, long writeOffset);

    void flush(CommandList commandList);

    void delete(CommandList commandList);

    void flip();
}
