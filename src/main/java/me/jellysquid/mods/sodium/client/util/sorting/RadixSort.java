package me.jellysquid.mods.sodium.client.util.sorting;

import java.util.Arrays;

/** Stable descending sort of float keys, preserving the merge sort's treatment of signed zero. */
final class RadixSort {
    private static final int SCRATCH_MAX = 1 << 16;
    private static final ThreadLocal<Scratch> SCRATCH = ThreadLocal.withInitial(Scratch::new);

    private RadixSort() {
    }

    /** Reused packed-key arrays and digit counts of one thread (the sort calls nothing outside this class). */
    private static final class Scratch {
        long[] a = new long[0], b = new long[0];
        final int[] counts = new int[4 * 256];
    }

    /**
     * The quad order Embeddium's MergeSort.mergeSort(keys) returns (indices, far quads first, ties in input order), or
     * null when a key is NaN (then the caller runs Embeddium's sort). Arrays shorter than 64 elements are also left to the
     * caller.
     */
    public static int[] sort(float[] keys) {
        int n = keys.length;
        if (n < 64) return null;
        Scratch s = n <= SCRATCH_MAX ? SCRATCH.get() : new Scratch();
        long[] a = s.a;
        if (a.length < n) s.a = a = new long[Math.max(n, Math.min(SCRATCH_MAX, a.length * 2))];
        for (int i = 0; i < n; i++) {
            float k = keys[i];
            if (k != k) {
                return null;
            }
            a[i] = ((long) descendingKey(k) << 32) | i;
        }
        if (s.b.length < a.length) s.b = new long[a.length];
        a = radix(a, s.b, s.counts, n);
        int[] out = new int[n];
        for (int i = 0; i < n; i++) out[i] = (int) a[i];
        return out;
    }

    /**
     * 32-bit pattern whose unsigned order is the descending numeric order of non-NaN floats; -0.0 and +0.0 map to the
     * same pattern (Embeddium's comparisons treat them as equal).
     */
    static int descendingKey(float k) {
        int b = k == 0.0f ? 0 : Float.floatToRawIntBits(k);
        return ~(b ^ ((b >> 31) | 0x80000000));
    }

    /**
     * Stable LSD radix sort of a[0..n) by the high 32 bits (unsigned), 8 bits per pass, b as the other buffer; returns
     * the array (a or b) that holds the sorted run.
     */
    private static long[] radix(long[] a, long[] b, int[] counts, int n) {
        Arrays.fill(counts, 0);
        for (int i = 0; i < n; i++) {
            int d = (int) (a[i] >>> 32);
            counts[d & 0xFF]++;
            counts[256 + ((d >>> 8) & 0xFF)]++;
            counts[512 + ((d >>> 16) & 0xFF)]++;
            counts[768 + (d >>> 24)]++;
        }
        for (int pass = 0; pass < 4; pass++) {
            int base = pass * 256;
            int shift = 32 + pass * 8;
            // a digit that every key shares leaves the order unchanged: skip the pass
            int first = (int) ((a[0] >>> shift) & 0xFF);
            if (counts[base + first] == n) continue;
            int sum = 0;
            for (int i = base; i < base + 256; i++) {
                int t = counts[i];
                counts[i] = sum;
                sum += t;
            }
            for (int i = 0; i < n; i++) {
                long v = a[i];
                b[counts[base + (int) ((v >>> shift) & 0xFF)]++] = v;
            }
            long[] t = a;
            a = b;
            b = t;
        }
        return a;
    }

}
