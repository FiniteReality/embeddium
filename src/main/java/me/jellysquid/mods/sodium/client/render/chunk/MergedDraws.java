package me.jellysquid.mods.sodium.client.render.chunk;

import me.jellysquid.mods.sodium.client.gl.device.MultiDrawBatch;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.system.Pointer;

/** Merges adjacent commands using the shared quad index buffer without changing their triangle order. */
final class MergedDraws {
    private MergedDraws() {}

    public static void merge(MultiDrawBatch batch) {
        int n = batch.size;
        if (n < 2 || Pointer.POINTER_SIZE != Long.BYTES) return;
        long pb = batch.pBaseVertex, pc = batch.pElementCount, pp = batch.pElementPointer;
        int base = MemoryUtil.memGetInt(pb), count = MemoryUtil.memGetInt(pc);
        long ptr = MemoryUtil.memGetAddress(pp);
        int out = 0;
        for (int i = 1; i < n; i++) {
            int b = MemoryUtil.memGetInt(pb + ((long) i << 2)), c = MemoryUtil.memGetInt(pc + ((long) i << 2));
            long p = MemoryUtil.memGetAddress(pp + ((long) i << 3));
            if (ptr == 0L && p == 0L && count >= 0 && c >= 0 && count % 6 == 0 && c % 6 == 0 && (long) base + (long) (count / 6) * 4L == (long) b
                    && (long) count + (long) c <= Integer.MAX_VALUE) {
                count += c;
            } else {
                MemoryUtil.memPutInt(pb + ((long) out << 2), base);
                MemoryUtil.memPutInt(pc + ((long) out << 2), count);
                MemoryUtil.memPutAddress(pp + ((long) out << 3), ptr);
                out++;
                base = b;
                count = c;
                ptr = p;
            }
        }
        MemoryUtil.memPutInt(pb + ((long) out << 2), base);
        MemoryUtil.memPutInt(pc + ((long) out << 2), count);
        MemoryUtil.memPutAddress(pp + ((long) out << 3), ptr);
        batch.size = out + 1;
    }

}
