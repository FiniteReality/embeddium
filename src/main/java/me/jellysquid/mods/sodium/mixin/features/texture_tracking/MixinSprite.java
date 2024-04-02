package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(TextureAtlasSprite.class)
public abstract class MixinSprite implements SpriteExtended {
    @Shadow @Final private float u0;
    private boolean active;

    @Override
    public void setActive(boolean active) {
        this.active = active;
    }

    @Override
    public boolean isActive() {
        return this.active;
    }

    /**
     * @author embeddedt
     * @reason Mark sprite as active for animation when U0 coordinate is retrieved. This catches some more render
     * paths not caught by the other mixins.
     */
    @Redirect(method = "getU0", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;u0:F"))
    private float embeddium$markActive(TextureAtlasSprite sprite) {
        this.active = true;
        return this.u0;
    }
}
