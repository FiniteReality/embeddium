package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlasSprite.class)
public class SpriteMixin {
    @Shadow
    @Final
    private SpriteContents contents;

    /**
     * @author embeddedt
     * @reason Mark sprite as active for animation when U0 coordinate is retrieved. This catches some more render
     * paths not caught by the other mixins.
     */
    @ModifyReturnValue(method = "getU0", at = @At("RETURN"))
    private float embeddium$markActive(float f) {
        ((SpriteContentsExtended)this.contents).sodium$setActive(true);
        return f;
    }
}
