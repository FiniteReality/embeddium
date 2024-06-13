package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.fabric.injectors.RenderTypeInjector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = RenderType.class, priority = 100)
public class RenderTypeMixin implements RenderTypeInjector {
    @Unique
    private int chunkLayerId = -1;

    @Inject(method = "<clinit>", at = @At("RETURN"))
    private static void assignLayerIds(CallbackInfo ci) {
        int i = 0;
        for(RenderType type : RenderType.chunkBufferLayers()) {
            ((RenderTypeMixin)(Object)type).chunkLayerId = i++;
        }
    }

    @Override
    public int getChunkLayerId() {
        return chunkLayerId;
    }
}
