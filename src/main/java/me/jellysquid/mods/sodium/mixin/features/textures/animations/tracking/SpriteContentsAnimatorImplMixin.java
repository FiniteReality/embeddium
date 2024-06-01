package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(TextureAtlasSprite.AnimatedTexture.class)
public class SpriteContentsAnimatorImplMixin {
    @Shadow
    int subFrame;

    @Shadow
    int frame;

    @Shadow
    @Final
    private List<TextureAtlasSprite.FrameInfo> frames;
    @Unique
    private TextureAtlasSprite parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(TextureAtlasSprite spriteContents, List<TextureAtlasSprite.FrameInfo> pFrames, int pFrameRowSize, TextureAtlasSprite.InterpolationData pInterpolationData, CallbackInfo ci) {
        this.parent = spriteContents;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            this.subFrame++;
            List<TextureAtlasSprite.FrameInfo> frames = this.frames;
            if (this.subFrame >= frames.get(this.frame).time) {
                this.frame = (this.frame + 1) % frames.size();
                this.subFrame = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;
        parent.sodium$setActive(false);
    }
}
