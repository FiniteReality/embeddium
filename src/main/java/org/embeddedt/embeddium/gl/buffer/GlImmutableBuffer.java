package org.embeddedt.embeddium.gl.buffer;

import org.embeddedt.embeddium.gl.util.EnumBitField;

public class GlImmutableBuffer extends GlBuffer {
    private final EnumBitField<GlBufferStorageFlags> flags;

    public GlImmutableBuffer(EnumBitField<GlBufferStorageFlags> flags) {
        this.flags = flags;
    }

    public EnumBitField<GlBufferStorageFlags> getFlags() {
        return this.flags;
    }
}
