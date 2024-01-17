package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteExtended;
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
public class MixinSpriteAnimation {
    @Shadow
    public int subFrame;

    @Shadow
    public int frame;

    @Shadow
    @Final
    public List<TextureAtlasSprite.FrameInfo> frames;

    @Unique
    private TextureAtlasSprite parent;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(TextureAtlasSprite parent, List frames, int frameCount, TextureAtlasSprite.InterpolationData interpolation, CallbackInfo ci) {
        this.parent = parent;
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void preTick(CallbackInfo ci) {
        SpriteExtended parent = (SpriteExtended) this.parent;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (onDemand && !parent.isActive()) {
            this.subFrame++;
            if (this.subFrame >= this.frames.get(this.frame).time) {
                this.frame = (this.frame + 1) % this.frames.size();
                this.subFrame = 0;
            }
            ci.cancel();
        }
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void postTick(CallbackInfo ci) {
        SpriteExtended parent = (SpriteExtended) this.parent;
        parent.setActive(false);
    }
}
