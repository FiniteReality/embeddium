package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlasSprite.class)
public abstract class SpriteContentsMixin implements SpriteContentsExtended {
    @Shadow
    @Final
    @Nullable
    private TextureAtlasSprite.AnimatedTexture animatedTexture;

    @Unique
    private boolean active;

    @Override
    public void sodium$setActive(boolean value) {
        this.active = value;
    }

    @Override
    public boolean sodium$hasAnimation() {
        return this.animatedTexture != null;
    }

    @Override
    public boolean sodium$isActive() {
        return this.active;
    }

    /**
     * @author embeddedt
     * @reason Mark sprite as active for animation when U0 coordinate is retrieved. This catches some more render
     * paths not caught by the other mixins.
     */
    @ModifyReturnValue(method = "getU0", at = @At("RETURN"))
    private float embeddium$markActive(float f) {
        this.active = true;
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
