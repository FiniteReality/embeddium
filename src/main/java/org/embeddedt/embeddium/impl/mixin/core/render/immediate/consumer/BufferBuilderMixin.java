package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import org.embeddedt.embeddium.api.memory.MemoryIntrinsics;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import org.embeddedt.embeddium.api.vertex.serializer.VertexSerializerRegistry;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.VertexFormat;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private ByteBufferBuilder buffer;

    @Shadow
    private int vertices;
    @Shadow
    @Final
    private int vertexSize;

    @Shadow
    private long vertexPointer;
    @Unique
    private VertexFormatDescription embeddiumFormat;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void onFormatChanged(ByteBufferBuilder buffer, VertexFormat.Mode mode, VertexFormat format, CallbackInfo ci) {
        this.embeddiumFormat = VertexFormatRegistry.instance().get(format);
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.embeddiumFormat != null && this.embeddiumFormat.isSimpleFormat() && this.buffer != null;
    }

    @Override
    public void push(MemoryStack stack, long src, int count, VertexFormatDescription format) {
        var length = count * this.vertexSize;

        // Ensure that there is space for the data we're about to push
        long dst = this.buffer.reserve(length);

        if (format == this.embeddiumFormat) {
            // The layout is the same, so we can just perform a memory copy
            // The stride of a vertex format is always 4 bytes, so this aligned copy is always safe
            MemoryIntrinsics.copyMemory(src, dst, length);
        } else {
            // The layout differs, so we need to perform a conversion on the vertex data
            this.copySlow(src, dst, count, format);
        }

        this.vertices += count;
        // TODO - probably not needed?
        this.vertexPointer = dst;
    }

    @Unique
    private void copySlow(long src, long dst, int count, VertexFormatDescription format) {
        VertexSerializerRegistry.instance()
                .get(format, this.embeddiumFormat)
                .serialize(src, dst, count);
    }
}
