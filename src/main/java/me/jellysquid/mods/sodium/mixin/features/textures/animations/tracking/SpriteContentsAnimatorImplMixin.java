package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
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

    @Unique
    private int[] frameTimes;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(SpriteContents spriteContents, SpriteContents.AnimatedTexture animation, SpriteContents.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = spriteContents;
        List<SpriteContents.FrameInfo> frames = ((SpriteContentsAnimationAccessor) animation).getFrames();
        Class<?> type = frames.getClass();
        // Mutable lists supplied by other mods must continue to be read each tick.
        if (frames instanceof ImmutableList<?> || type == List.of(0).getClass()
                || type == List.of(0, 1, 2).getClass()) {
            int[] times = new int[frames.size()];
            for (int i = 0; i < times.length; i++) {
                if (frames.get(i) == null) {
                    return;
                }
                times[i] = ((SpriteContentsAnimationFrameAccessor) frames.get(i)).getTime();
            }
            this.frameTimes = times;
        }
    }

    @Inject(method = "tickAndUpload", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteContentsExtended parent = (SpriteContentsExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.sodium$isActive()) {
            this.subFrame++;
            int[] times = this.frameTimes;
            if (times != null && this.frame >= 0 && this.frame < times.length) {
                if (this.subFrame >= times[this.frame]) {
                    this.frame = (this.frame + 1) % times.length;
                    this.subFrame = 0;
                }
            } else {
                List<SpriteContents.FrameInfo> frames = ((SpriteContentsAnimationAccessor) this.animationInfo).getFrames();
                if (this.subFrame >= ((SpriteContentsAnimationFrameAccessor) frames.get(this.frame)).getTime()) {
                    this.frame = (this.frame + 1) % frames.size();
                    this.subFrame = 0;
                }
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
