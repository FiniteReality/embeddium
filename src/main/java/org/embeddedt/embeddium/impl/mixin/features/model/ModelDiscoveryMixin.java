package org.embeddedt.embeddium.impl.mixin.features.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelDiscovery;
import net.minecraft.client.resources.model.UnbakedModel;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.impl.model.EpsilonizableBlockElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelDiscovery.class)
public class ModelDiscoveryMixin {
    @Inject(method = "loadBlockModel", at = @At("RETURN"))
    private void epsilonizeBlockModel(ResourceLocation pLocation, CallbackInfoReturnable<UnbakedModel> cir) {
        if(pLocation.getPath().startsWith("block/")) {
            if(cir.getReturnValue() instanceof BlockModel bm) {
                try {
                    for(BlockElement vanillaElement : bm.getElements()) {
                        ((EpsilonizableBlockElement)vanillaElement).embeddium$epsilonize();
                    }
                } catch(Throwable ignored) {
                    // Epsilonizing isn't critical, make sure it doesn't prevent resource reload
                }
            }
        }
    }
}
