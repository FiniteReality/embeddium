package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import org.embeddedt.embeddium.render.EmbeddiumRenderLayerCache;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemBlockRenderTypes.class)
public class MixinRenderLayers {
    @Inject(method = { "setRenderLayer(Lnet/minecraft/world/level/block/Block;Ljava/util/function/Predicate;)V", "setRenderLayer(Lnet/minecraft/world/level/material/Fluid;Ljava/util/function/Predicate;)V" }, at = @At("RETURN"), remap = false)
    private static void onRenderLayerPredicateChanged(CallbackInfo ci) {
        Minecraft.getInstance().execute(() -> {
            if (Minecraft.getInstance().levelRenderer == null) {
                // There is no active world, so nothing to invalidate
                return;
            }
            SodiumWorldRenderer renderer = SodiumWorldRenderer.instanceNullable();
            if (renderer != null) {
                for (ChunkRenderCacheLocal cache : renderer.getActiveChunkRenderCaches()) {
                    cache.getRenderLayerCache().invalidate();
                }
            }
        });
    }
}
