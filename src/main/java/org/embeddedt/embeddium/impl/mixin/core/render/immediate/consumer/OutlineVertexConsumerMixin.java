package org.embeddedt.embeddium.impl.mixin.core.render.immediate.consumer;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.vertex.attributes.CommonVertexAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "net/minecraft/client/renderer/OutlineBufferSource$EntityOutlineGenerator")
public abstract class OutlineVertexConsumerMixin implements VertexBufferWriter {
    @Shadow
    @Final
    private VertexConsumer delegate;

    @Shadow
    @Final
    private int color;
    @Unique
    private boolean isFullWriter;

    @Inject(method = "<init>(Lcom/mojang/blaze3d/vertex/VertexConsumer;I)V", at = @At("RETURN"))
    private void onInit(CallbackInfo ci) {
        this.isFullWriter = VertexBufferWriter.tryOf(this.delegate) != null;
    }

    @Override
    public boolean canUseIntrinsics() {
        return this.isFullWriter;
    }

    @Override
    public void push(MemoryStack stack, long ptr, int count, VertexFormatDescription format) {
        transform(ptr, count, format, ColorARGB.toABGR(this.color));

        VertexBufferWriter.of(this.delegate)
                .push(stack, ptr, count, format);
    }

    /**
     * Transforms the color element of each vertex to use the specified value.
     *
     * @param ptr    The buffer of vertices to transform
     * @param count  The number of vertices to transform
     * @param format The format of the vertices
     * @param color  The packed color to use for transforming the vertices
     */
    @Unique
    private static void transform(long ptr, int count, VertexFormatDescription format,
                                  int color) {
        long stride = format.stride();
        long offsetColor = format.getElementOffset(CommonVertexAttribute.COLOR);

        for (int vertexIndex = 0; vertexIndex < count; vertexIndex++) {
            ColorAttribute.set(ptr + offsetColor, color);
            ptr += stride;
        }
    }

}