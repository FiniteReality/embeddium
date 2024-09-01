package me.jellysquid.mods.sodium.mixin.features.textures;

import com.mojang.blaze3d.platform.NativeImage;
import org.embeddedt.embeddium.impl.mixinterface.NativeImageAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NativeImage.class)
public class NativeImageMixin implements NativeImageAccessor {
    @Shadow
    private long pixels;

    @Override
    public long embeddium$getPixels() {
        return this.pixels;
    }
}
