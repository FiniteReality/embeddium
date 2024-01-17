package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder;

import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferVertexConsumer;
import com.mojang.blaze3d.vertex.DefaultedVertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import me.jellysquid.mods.sodium.client.buffer.ExtendedVertexFormat;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BufferBuilder.class)
public abstract class BufferBuilderMixin extends DefaultedVertexConsumer implements BufferVertexConsumer {
    @Shadow
    private VertexFormatElement currentElement;

    @Shadow
    private int nextElementByte;

    @Shadow
    private int elementIndex;

    private ExtendedVertexFormat.Element[] embeddium$vertexFormatExtendedElements;
    private ExtendedVertexFormat.Element embeddium$currentExtendedElement;

    @Inject(method = "switchFormat",
            at = @At(
                    value = "FIELD",
                    target = "Lcom/mojang/blaze3d/vertex/BufferBuilder;format:Lcom/mojang/blaze3d/vertex/VertexFormat;",
                    opcode = Opcodes.PUTFIELD
            )
    )
    private void onFormatChanged(VertexFormat format, CallbackInfo ci) {
        embeddium$vertexFormatExtendedElements = ((ExtendedVertexFormat) format).embeddium$getExtendedElements();
        embeddium$currentExtendedElement = embeddium$vertexFormatExtendedElements[0];
    }

    /**
     * @author JellySquid
     * @reason Remove modulo operations, recursion, and list dereference
     */
    @Override
    @Overwrite
    public void nextElement() {
        if ((elementIndex += embeddium$currentExtendedElement.increment) >= embeddium$vertexFormatExtendedElements.length)
            elementIndex -= embeddium$vertexFormatExtendedElements.length;
        nextElementByte += embeddium$currentExtendedElement.byteLength;
        embeddium$currentExtendedElement = embeddium$vertexFormatExtendedElements[elementIndex];
        currentElement = embeddium$currentExtendedElement.actual;

        if (this.defaultColorSet && this.currentElement.getUsage() == VertexFormatElement.Usage.COLOR) {
            BufferVertexConsumer.super.color(this.defaultR, this.defaultG, this.defaultB, this.defaultA);
        }
    }
}
