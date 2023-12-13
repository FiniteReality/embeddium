package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.RenderLayers;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(RenderLayers.class)
public class MixinRenderLayers {
    // Note: The Sodium patch to replace the backing collection is removed, because Forge now uses fastutil here

    @Unique
    private static boolean embeddium$leavesFancy;

    @Redirect(
            method = { "getBlockLayer", "getMovingBlockLayer", "canRenderInLayer(Lnet/minecraft/block/BlockState;Lnet/minecraft/client/render/RenderLayer;)Z" },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderLayers;fancyGraphicsOrBetter:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return embeddium$leavesFancy;
    }

    @Inject(method = "setFancyGraphicsOrBetter", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        embeddium$leavesFancy = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphicsOrBetter ? GraphicsMode.FANCY : GraphicsMode.FAST);
    }
}
