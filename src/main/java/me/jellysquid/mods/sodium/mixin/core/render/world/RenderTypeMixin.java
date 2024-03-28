package me.jellysquid.mods.sodium.mixin.core.render.world;

import net.minecraft.client.renderer.RenderType;
import org.embeddedt.embeddium.render.type.RenderTypeExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(RenderType.class)
public class RenderTypeMixin implements RenderTypeExtended {
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
    public int embeddium$getChunkLayerId() {
        return chunkLayerId;
    }
}
