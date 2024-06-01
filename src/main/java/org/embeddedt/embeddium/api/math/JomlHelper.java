package org.embeddedt.embeddium.api.math;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

public class JomlHelper {
    public static void set(Matrix4f dst, com.mojang.math.Matrix4f src) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.callocFloat(16);
            src.store(buffer);

            dst.set(buffer);
        }
    }

    public static Matrix4f copy(com.mojang.math.Matrix4f src) {
        Matrix4f dst = new Matrix4f();
        set(dst, src);

        return dst;
    }

    public static Quaternionf copy(Quaternionf dst, com.mojang.math.Quaternion src) {
        dst.set(src.i(), src.j(), src.k(), src.r());
        return dst;
    }
}
