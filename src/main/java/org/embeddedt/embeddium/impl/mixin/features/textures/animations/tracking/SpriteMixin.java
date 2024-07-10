package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
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
        SpriteUtil.markSpriteActive((TextureAtlasSprite)(Object)this);
        return f;
    }

    /**
     * @author embeddedt
     * @reason Mark sprite as active for animation when U coordinate is retrieved. This catches some more render
     * paths not caught by the other mixins.
     */
    @ModifyReturnValue(method = "getU", at = @At("RETURN"))
    private float embeddium$markActiveInterpolated(float f) {
        SpriteUtil.markSpriteActive((TextureAtlasSprite)(Object)this);
        return f;
    }
}
