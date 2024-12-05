package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import org.embeddedt.embeddium.impl.Embeddium;
import org.embeddedt.embeddium.impl.render.texture.SpriteContentsExtended;
import net.minecraft.client.renderer.texture.SpriteContents;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(SpriteContents.Ticker.class)
public class SpriteContentsAnimatorImplMixin {
    @Shadow
    int subFrame;
    @Shadow
    @Final
    SpriteContents.AnimatedTexture animationInfo;
    @Shadow
    int frame;

    @Unique
    private SpriteContents parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents spriteContents, SpriteContents.AnimatedTexture animation, SpriteContents.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = spriteContents;
    }

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;

        boolean onDemand = Embeddium.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            this.subFrame++;
            List<SpriteContents.FrameInfo> frames = ((SpriteContentsAnimationAccessor)this.animationInfo).getFrames();
            if (this.subFrame >= ((SpriteContentsAnimationFrameAccessor)(Object)frames.get(this.frame)).getTime()) {
                this.frame = (this.frame + 1) % frames.size();
                this.subFrame = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "tickAndUpload", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;
        parent.sodium$setActive(false);
    }
}
