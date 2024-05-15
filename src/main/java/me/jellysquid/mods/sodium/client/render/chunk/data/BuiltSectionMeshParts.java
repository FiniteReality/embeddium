package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import org.embeddedt.embeddium.render.chunk.sorting.TranslucentQuadAnalyzer;
import org.jetbrains.annotations.Nullable;

public class BuiltSectionMeshParts {
    private final VertexRange[] ranges;
    private final NativeBuffer buffer;
    private final NativeBuffer indexBuffer;
    private final TranslucentQuadAnalyzer.SortState sortState;

    public BuiltSectionMeshParts(NativeBuffer buffer, @Nullable NativeBuffer indexBuffer, TranslucentQuadAnalyzer.SortState sortState, VertexRange[] ranges) {
        this.ranges = ranges;
        this.buffer = buffer;
        this.indexBuffer = indexBuffer;
        this.sortState = sortState;
    }

    public NativeBuffer getVertexData() {
        return this.buffer;
    }

    @Nullable
    public NativeBuffer getIndexData() {
        return this.indexBuffer;
    }

    public VertexRange[] getVertexRanges() {
        return this.ranges;
    }

    public TranslucentQuadAnalyzer.SortState getSortState() {
        return this.sortState;
    }
}
