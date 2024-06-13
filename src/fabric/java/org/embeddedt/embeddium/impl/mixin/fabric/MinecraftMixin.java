package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.embeddedt.embeddium.fabric.init.EmbeddiumFabricInitializer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Inject(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/Window;updateVsync(Z)V"))
    private void registerReloadListeners(CallbackInfo ci) {
        try {
            var clz = Class.forName("org.embeddedt.embeddium.impl.render.frapi.SpriteFinderCache");
            var method = clz.getDeclaredMethod("onReload", RegisterClientReloadListenersEvent.class);
            method.invoke(null, new RegisterClientReloadListenersEvent());
        } catch(ReflectiveOperationException e) {
            EmbeddiumFabricInitializer.LOGGER.error("Failed to set up sprite finder", e);
        }
    }
}
