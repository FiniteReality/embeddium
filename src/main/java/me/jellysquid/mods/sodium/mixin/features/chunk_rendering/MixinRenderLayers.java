package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import net.minecraft.client.render.RenderLayers;
import org.embeddedt.embeddium.render.EmbeddiumRenderLayerCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderLayers.class)
public class MixinRenderLayers {
    @Inject(method = { "setRenderLayer(Lnet/minecraft/block/Block;Ljava/util/function/Predicate;)V", "setRenderLayer(Lnet/minecraft/fluid/Fluid;Ljava/util/function/Predicate;)V" }, at = @At("RETURN"))
    private static void onRenderLayerPredicateChanged(CallbackInfo ci) {
        EmbeddiumRenderLayerCache.invalidate();
    }
}
