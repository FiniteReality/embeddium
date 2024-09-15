package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.client.renderer.block.model.BlockElement;
import net.minecraft.client.renderer.block.model.BlockModel;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.model.EpsilonizableBlockElement;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ModelBakery.class)
public class ModelBakeryMixin {
    @Inject(method = "loadBlockModel", at = @At("RETURN"))
    private void epsilonizeBlockModel(ResourceLocation pLocation, CallbackInfoReturnable<BlockModel> cir) {
        if(pLocation.getPath().startsWith("block/")) {
            BlockModel bm = cir.getReturnValue();
            if(bm != null) {
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
