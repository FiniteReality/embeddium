package org.embeddedt.embeddium.impl.mixin.features.options.overlays;

import org.embeddedt.embeddium.impl.Embeddium;
import net.minecraft.client.gui.Gui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Gui.class)
public class InGameHudMixin {
    @Redirect(method = "renderCameraOverlays", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectFancyGraphicsVignette() {
        return Embeddium.options().quality.enableVignette;
    }
}
