package me.jellysquid.mods.sodium.client.util.math;

import org.lwjgl.system.MemoryStack;

import repack.joml.Matrix4f;

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
}
