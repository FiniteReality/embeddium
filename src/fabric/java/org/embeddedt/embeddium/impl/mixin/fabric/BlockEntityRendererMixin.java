package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import org.embeddedt.embeddium.fabric.injectors.BlockEntityRendererInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BlockEntityRenderer.class)
public interface BlockEntityRendererMixin extends BlockEntityRendererInjector {
}
