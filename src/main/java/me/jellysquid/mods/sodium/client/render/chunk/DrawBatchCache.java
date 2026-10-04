package me.jellysquid.mods.sodium.client.render.chunk;

import java.lang.reflect.Field;
import org.lwjgl.system.Pointer;
import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.util.BitwiseMath;

/** The last two draw command lists for one region and terrain pass, invalidated whenever its mesh data changes. */
public final class DrawBatchCache {
    private static final sun.misc.Unsafe U;
    private static final long INT_BASE, LONG_BASE;
    static {
        try {
            Field field = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            U = (sun.misc.Unsafe) field.get(null);
        } catch (ReflectiveOperationException e) {
            throw new ExceptionInInitializerError(e);
        }
        INT_BASE = U.arrayBaseOffset(int[].class);
        LONG_BASE = U.arrayBaseOffset(long[].class);
    }

    private DrawBatchCache() {}

    /**
     * The comparison bits Embeddium's getVisibleFaces computes for every section coordinate of the region: for each
     * x (8), y (4) and z (8) the "greater than min - 3" and "less than max + 3" tests of the camera's integer position.
     * Two cameras with equal bits give every section of the region the same visible-face mask.
     */
    public static long faceBits(RenderRegion region, CameraTransform camera) {
        long bits = 0L;
        int bit = 0;
        int cx = camera.intX, cy = camera.intY, cz = camera.intZ;
        int ox = region.getChunkX(), oy = region.getChunkY(), oz = region.getChunkZ();
        for (int i = 0; i < RenderRegion.REGION_WIDTH; i++) {
            int min = ox + i << 4;
            bits |= (long) BitwiseMath.greaterThan(cx, min - 3) << bit++;
            bits |= (long) BitwiseMath.lessThan(cx, min + 16 + 3) << bit++;
        }
        for (int i = 0; i < RenderRegion.REGION_HEIGHT; i++) {
            int min = oy + i << 4;
            bits |= (long) BitwiseMath.greaterThan(cy, min - 3) << bit++;
            bits |= (long) BitwiseMath.lessThan(cy, min + 16 + 3) << bit++;
        }
        for (int i = 0; i < RenderRegion.REGION_LENGTH; i++) {
            int min = oz + i << 4;
            bits |= (long) BitwiseMath.greaterThan(cz, min - 3) << bit++;
            bits |= (long) BitwiseMath.lessThan(cz, min + 16 + 3) << bit++;
        }
        return bits;
    }

    /** Per SectionRenderDataStorage: the commands of its last two fills. */
    public static final class Slots {
        private boolean enabled = Pointer.POINTER_SIZE == Long.BYTES;

        public boolean isEnabled() {
            return this.enabled;
        }

        public void disable() {
            this.enabled = false;
            this.invalidate();
        }

        public void invalidate() {
            this.a.valid = this.b.valid = false;
        }
        final Entry a = new Entry(), b = new Entry();
        long uses;

        /** The entry whose inputs equal these, or null. */
        public Entry find(int listCount, ChunkRenderList renderList, boolean reverse, boolean sorted, boolean culling, boolean merged, long faceBits) {
            if (!this.enabled) return null;
            if (this.a.same(listCount, renderList, reverse, sorted, culling, merged, faceBits)) return this.a.touch(++this.uses);
            if (this.b.same(listCount, renderList, reverse, sorted, culling, merged, faceBits)) return this.b.touch(++this.uses);
            return null;
        }

        /** The entry to overwrite with a fresh fill (the least recently used one). */
        public Entry victim() {
            return (this.a.lastUse <= this.b.lastUse ? this.a : this.b).touch(++this.uses);
        }
    }

    /** One remembered fill: its inputs and the commands in [0, size). */
    public static final class Entry {
        boolean valid;
        int listCount;
        final byte[] list = new byte[RenderRegion.REGION_SIZE];
        boolean reverse, sorted, culling, merged;
        long faceBits;
        int size;
        int[] baseVertex = new int[16], elementCount = new int[16];
        long[] elementPointer = new long[16];
        long lastUse;

        Entry touch(long stamp) {
            this.lastUse = stamp;
            return this;
        }

        boolean same(int listCount, ChunkRenderList renderList, boolean reverse, boolean sorted, boolean culling, boolean merged, long faceBits) {
            return this.valid && this.listCount == listCount && this.reverse == reverse && this.sorted == sorted
                    && this.culling == culling && this.merged == merged && this.faceBits == faceBits
                    && renderList.matchesGeometry(this.list, listCount);
        }

        /**
         * Copies the batch's commands [0, size) and the inputs they were filled from.
         */
        public void store(MultiDrawBatch batch, int listCount, ChunkRenderList renderList, boolean reverse, boolean sorted, boolean culling, boolean merged,
                          long faceBits) {
            int n = batch.size;
            if (n > this.baseVertex.length) {
                int cap = Math.max(n, this.baseVertex.length * 2);
                this.baseVertex = new int[cap];
                this.elementCount = new int[cap];
                this.elementPointer = new long[cap];
            }
            if (n > 0) {
                U.copyMemory(null, batch.pBaseVertex, this.baseVertex, INT_BASE, (long) n << 2);
                U.copyMemory(null, batch.pElementCount, this.elementCount, INT_BASE, (long) n << 2);
                U.copyMemory(null, batch.pElementPointer, this.elementPointer, LONG_BASE, (long) n << 3);
            }
            this.size = n;
            this.listCount = listCount;
            renderList.copyGeometry(this.list);
            this.reverse = reverse;
            this.sorted = sorted;
            this.culling = culling;
            this.merged = merged;
            this.faceBits = faceBits;
            this.valid = true;
        }

        /** Writes the remembered commands into the batch: batch.size and [0, size) of its three arrays. */
        public void load(MultiDrawBatch batch) {
            int n = this.size;
            if (n > 0) {
                U.copyMemory(this.baseVertex, INT_BASE, null, batch.pBaseVertex, (long) n << 2);
                U.copyMemory(this.elementCount, INT_BASE, null, batch.pElementCount, (long) n << 2);
                U.copyMemory(this.elementPointer, LONG_BASE, null, batch.pElementPointer, (long) n << 3);
            }
            batch.size = n;
        }

    }
}
