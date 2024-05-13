package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

public class BuiltSectionMeshParts {
    private final VertexRange[] ranges;
    private final NativeBuffer buffer;
    private final NativeBuffer indexBuffer;

    public BuiltSectionMeshParts(NativeBuffer buffer, NativeBuffer indexBuffer, VertexRange[] ranges) {
        this.ranges = ranges;
        this.buffer = buffer;
        this.indexBuffer = indexBuffer;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    public NativeBuffer getIndexData() {
        return this.indexBuffer;
    }

    public VertexRange[] getVertexRanges() {
        return this.ranges;
    }
}
