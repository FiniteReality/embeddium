package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.gl.arena.GlBufferSegment;
import me.jellysquid.mods.sodium.client.render.chunk.DrawBatchCache;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class SectionRenderDataStorage {
    private final GlBufferSegment[] allocations = new GlBufferSegment[RenderRegion.REGION_SIZE];
    private final GlBufferSegment[] indexAllocations = new GlBufferSegment[RenderRegion.REGION_SIZE];

    private final long pMeshDataArray;
    private final DrawBatchCache.Slots drawBatchCache = new DrawBatchCache.Slots();

    public DrawBatchCache.Slots getDrawBatchCache() {
        return this.drawBatchCache;
    }

    public SectionRenderDataStorage() {
        this.pMeshDataArray = SectionRenderDataUnsafe.allocateHeap(RenderRegion.REGION_SIZE);
    }

    public void setMeshes(int localSectionIndex,
                          GlBufferSegment allocation, @Nullable GlBufferSegment indexAllocation, VertexRange[] ranges) {
        this.drawBatchCache.invalidate();
        if (this.allocations[localSectionIndex] != null) {
            this.allocations[localSectionIndex].delete();
            this.allocations[localSectionIndex] = null;
        }

        if (this.indexAllocations[localSectionIndex] != null) {
            this.indexAllocations[localSectionIndex].delete();
            this.indexAllocations[localSectionIndex] = null;
        }

        this.allocations[localSectionIndex] = allocation;
        this.indexAllocations[localSectionIndex] = indexAllocation;

        var pMeshData = this.getInternalDataPointer(localSectionIndex);

        int sliceMask = 0;
        int vertexOffset = allocation.getOffset();
        int indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        for (int facingIndex = 0; facingIndex < ModelQuadFacing.COUNT; facingIndex++) {
            VertexRange vertexRange = ranges[facingIndex];
            int vertexCount;

            if (vertexRange != null) {
                vertexCount = vertexRange.vertexCount();
            } else {
                vertexCount = 0;
            }

            int indexCount = (vertexCount >> 2) * 6;

            SectionRenderDataUnsafe.setVertexOffset(pMeshData, facingIndex, vertexOffset);
            SectionRenderDataUnsafe.setElementCount(pMeshData, facingIndex, indexCount);
            SectionRenderDataUnsafe.setIndexOffset(pMeshData, facingIndex, indexOffset);

            if (vertexCount > 0) {
                sliceMask |= 1 << facingIndex;
            }

            vertexOffset += vertexCount;
            indexOffset += indexCount * 4;
        }

        SectionRenderDataUnsafe.setSliceMask(pMeshData, sliceMask);
    }

    public void removeMeshes(int localSectionIndex) {
        this.drawBatchCache.invalidate();
        if (this.allocations[localSectionIndex] != null) {
            this.allocations[localSectionIndex].delete();
            this.allocations[localSectionIndex] = null;

            SectionRenderDataUnsafe.clear(this.getInternalDataPointer(localSectionIndex));
        }

        removeIndexBuffer(localSectionIndex);
    }

    public void removeIndexBuffer(int localSectionIndex) {
        this.drawBatchCache.invalidate();
        if (this.indexAllocations[localSectionIndex] != null) {
            this.indexAllocations[localSectionIndex].delete();
            this.indexAllocations[localSectionIndex] = null;
        }
    }

    public void replaceIndexBuffer(int localSectionIndex, GlBufferSegment indexAllocation) {
        this.drawBatchCache.invalidate();
        removeIndexBuffer(localSectionIndex);

        this.indexAllocations[localSectionIndex] = indexAllocation;

        var pMeshData = this.getInternalDataPointer(localSectionIndex);

        int indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        for (int facingIndex = 0; facingIndex < ModelQuadFacing.COUNT; facingIndex++) {
            SectionRenderDataUnsafe.setIndexOffset(pMeshData, facingIndex, indexOffset);
            int indexCount = SectionRenderDataUnsafe.getElementCount(pMeshData, facingIndex);
            indexOffset += indexCount * 4;
        }
    }

    public void onBufferResized() {
        this.drawBatchCache.invalidate();
        for (int sectionIndex = 0; sectionIndex < RenderRegion.REGION_SIZE; sectionIndex++) {
            this.updateMeshes(sectionIndex);
        }
    }

    private void updateMeshes(int sectionIndex) {
        var allocation = this.allocations[sectionIndex];

        if (allocation == null) {
            return;
        }

        var indexAllocation = this.indexAllocations[sectionIndex];

        var vertexOffset = allocation.getOffset();
        var indexOffset = indexAllocation != null ? indexAllocation.getOffset() * 4 : 0;

        var data = this.getInternalDataPointer(sectionIndex);

        for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
            SectionRenderDataUnsafe.setVertexOffset(data, facing, vertexOffset);
            SectionRenderDataUnsafe.setIndexOffset(data, facing, indexOffset);

            var indexCount = SectionRenderDataUnsafe.getElementCount(data, facing);
            vertexOffset += (indexCount / 6) * 4; // convert elements back into vertices
            indexOffset += indexCount * 4;
        }
    }

    /**
     * Exposes a mutable pointer. The caller may retain it, so command caching stays disabled for this storage.
     */
    public long getDataPointer(int sectionIndex) {
        this.drawBatchCache.disable();
        return this.getInternalDataPointer(sectionIndex);
    }

    /**
     * Returns a pointer for reading render data only. Callers must not write through this pointer;
     * use the storage mutation methods or {@link #getDataPointer(int)} for mutable access.
     */
    public long getDataPointerForRead(int sectionIndex) {
        return this.getInternalDataPointer(sectionIndex);
    }

    private long getInternalDataPointer(int sectionIndex) {
        return SectionRenderDataUnsafe.heapPointer(this.pMeshDataArray, sectionIndex);
    }

    public void delete() {
        this.drawBatchCache.invalidate();
        for (var allocation : this.allocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        for (var allocation : this.indexAllocations) {
            if (allocation != null) {
                allocation.delete();
            }
        }

        Arrays.fill(this.allocations, null);
        Arrays.fill(this.indexAllocations, null);

        SectionRenderDataUnsafe.freeHeap(this.pMeshDataArray);
    }
}