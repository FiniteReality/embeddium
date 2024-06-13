package org.embeddedt.embeddium.impl.mixin.fabric;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.fabric.injectors.VertexConsumerInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(VertexConsumer.class)
public interface VertexConsumerMixin extends VertexConsumerInjector {
}
