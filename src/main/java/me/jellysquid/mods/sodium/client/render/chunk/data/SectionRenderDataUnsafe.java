package me.jellysquid.mods.sodium.client.render.chunk.data;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import org.lwjgl.system.MemoryUtil;

// This code is a terrible hack to get around the fact that we are so incredibly memory bound, and that we
// have no control over memory layout. The chunk rendering code spends an astronomical amount of time chasing
// object pointers that are scattered across the heap. Worse yet, because render state is initialized over a long
// period of time as the world loads, those objects are never even remotely close to one another in heap, so
// you also have to pay the penalty of a DTLB miss on every other access.
//
// Unfortunately, Hotspot *still* produces abysmal machine code for the chunk rendering code paths, since any usage of
// unsafe memory intrinsics seems to cause it to become paranoid about memory aliasing. Well, that, and it just produces
// terrible machine code in pretty much every critical code path we seem to have...
//
// Please never try to write performance critical code in Java. This is what it will do to you. And you will still be
// three times slower than the most naive solution in literally any other language that LLVM can compile.
//
// Memory layout:
// u64 slice_mask;
// u32 vertex_offset;
// u32 element_count;
// u32 index_offset;
public class SectionRenderDataUnsafe {
    private static final long OFFSET_SLICE_MASK = 0;
    private static final long OFFSET_SLICE_RANGES = 8;

    private static final long DATA_PER_FACING_SIZE = 12;
    private static final long NUM_FACINGS = 7; // 6 directions + UNASSIGNED

    private static final long STRIDE = 8 + (DATA_PER_FACING_SIZE * NUM_FACINGS);

    public static long allocateHeap(int count) {
        return MemoryUtil.nmemCalloc(count, STRIDE);
    }

    public static void freeHeap(long pointer) {
        MemoryUtil.nmemFree(pointer);
    }

    public static void clear(long pointer) {
        MemoryUtil.memSet(pointer, 0x0, STRIDE);
    }

    public static long heapPointer(long ptr, int index) {
        return ptr + (index * STRIDE);
    }

    public static void setSliceMask(long ptr, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_MASK, value);
    }

    public static int getSliceMask(long ptr) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_MASK);
    }

    public static void setVertexOffset(long ptr, int facing, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 0L, value);
    }

    public static int getVertexOffset(long ptr, int facing) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 0L);
    }

    public static void setElementCount(long ptr, int facing, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 4L, value);
    }

    public static int getElementCount(long ptr, int facing) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 4L);
    }

    public static void setIndexOffset(long ptr, int facing, int value) {
        MemoryUtil.memPutInt(ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 8L, value);
    }

    public static int getIndexOffset(long ptr, int facing) {
        return MemoryUtil.memGetInt(ptr + OFFSET_SLICE_RANGES + (facing * DATA_PER_FACING_SIZE) + 8L);
    }
}