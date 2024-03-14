package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder.fast_delegate;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.ExtendedBufferBuilder;
import me.jellysquid.mods.sodium.client.render.vertex.buffer.SodiumBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

// The core mixin that actually causes a SodiumBufferBuilder to be injected into render paths
@Mixin(MultiBufferSource.BufferSource.class)
public class BufferSourceMixin {
    @ModifyReturnValue(method = "getBuffer", at = @At("RETURN"))
    private VertexConsumer useFasterVertexConsumer(VertexConsumer vertexConsumer) {
        if (vertexConsumer instanceof ExtendedBufferBuilder bufferBuilder) {
            SodiumBufferBuilder replacement = bufferBuilder.sodium$getDelegate();
            if (replacement != null) {
                return replacement;
            }
        }
        return vertexConsumer;
    }

    @ModifyVariable(method = "method_24213", at = @At(value = "LOAD", ordinal = 0))
    private VertexConsumer changeComparedVertexConsumer(VertexConsumer input) {
        if (input instanceof SodiumBufferBuilder replacement) {
            return replacement.getOriginalBufferBuilder();
        } else {
            return input;
        }
    }
}