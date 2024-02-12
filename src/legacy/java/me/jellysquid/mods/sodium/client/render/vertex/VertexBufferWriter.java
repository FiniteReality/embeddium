package me.jellysquid.mods.sodium.client.render.vertex;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.lwjgl.system.MemoryStack;

@Deprecated
public interface VertexBufferWriter extends net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter {
    @Deprecated
    static VertexBufferWriter of(VertexConsumer consumer) {
        return (VertexBufferWriter) consumer;
    }

    /**
     * Copy the vertices from the source buffer and pushes them into this vertex buffer. The vertex buffer
     * is advanced by {@param count} vertices. If the vertex format differs from the target format of the
     * vertex buffer, a conversion will be performed.
     * <p>
     * After calling this function, the contents of {@param ptr} are undefined.
     * <p>
     * If {@param stack} is used by the caller, the stack frame will not be pushed/popped. This function should
     * only be called in a try-with-resources block with the provided stack, otherwise it could leak memory.
     *
     * @param stack  The memory stack which can be used as scratch memory
     * @param ptr    The pointer to read vertices from
     * @param count  The number of vertices to copy
     * @param format The format of the vertices
     */
    @Deprecated
    void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format);

    default void push(MemoryStack stack, long ptr, int count, net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription format) {
        push(stack, ptr, count, VertexFormatDescription.translateModern(format));
    }

    default boolean canUseIntrinsics() {
        return true;
    }
}
