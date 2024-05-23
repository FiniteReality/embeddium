package org.embeddedt.embeddium.mixin.features.options.weather;

import org.embeddedt.embeddium.Embeddium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    @Redirect(method = "renderSnowAndRain", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;useFancyGraphics()Z"))
    private boolean redirectGetFancyWeather() {
        return Embeddium.options().quality.weatherQuality.isFancy(Minecraft.getInstance().options.graphicsMode().get());
    }
}