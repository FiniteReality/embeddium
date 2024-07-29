package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.blaze3d.systems.RenderSystem;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.texture.SpriteContentsExtended;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.metadata.animation.AnimationMetadataSection;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(TextureAtlasSprite.class)
public abstract class SpriteContentsMixin implements SpriteContentsExtended {
    @Unique
    private boolean active;

    @Override
    public void sodium$setActive(boolean value) {
        this.active = value;
    }

    @Override
    public boolean sodium$hasAnimation() {
        return isAnimation();
    }

    @Override
    public boolean sodium$isActive() {
        return this.active;
    }

    @Shadow
    private int subFrame;

    @Shadow
    @Final
    private AnimationMetadataSection metadata;

    @Shadow
    private int frame;

    @Shadow
    public abstract int getFrameCount();

    @Shadow
    protected abstract void upload(int int_1);

    @Shadow
    @Final
    private TextureAtlasSprite.InterpolationData interpolationData;

    @Shadow @Final private float u0;

    @Shadow
    public abstract boolean isAnimation();

    /**
     * @author JellySquid
     * @reason Allow conditional texture updating
     */
    @Overwrite
    public void cycleFrames() {
        this.subFrame++;

        boolean onDemand = SodiumClientMod.options().performance.animateOnlyVisibleTextures;

        if (!onDemand || this.active) {
            this.uploadTexture();
        } else {
            // Check and update the frame index anyway to avoid getting out of sync
            if (this.subFrame >= this.metadata.getFrameTime(this.frame)) {
                int frameCount = this.metadata.getFrameCount() == 0 ? this.getFrameCount() : this.metadata.getFrameCount();
                this.frame = (this.frame + 1) % frameCount;
                this.subFrame = 0;
            }
        }
    }

    private void uploadTexture() {
        if (this.subFrame >= this.metadata.getFrameTime(this.frame)) {
            int prevFrameIndex = this.metadata.getFrameIndex(this.frame);
            int frameCount = this.metadata.getFrameCount() == 0 ? this.getFrameCount() : this.metadata.getFrameCount();

            this.frame = (this.frame + 1) % frameCount;
            this.subFrame = 0;

            int frameIndex = this.metadata.getFrameIndex(this.frame);

            if (prevFrameIndex != frameIndex && frameIndex >= 0 && frameIndex < this.getFrameCount()) {
                this.upload(frameIndex);
            }
        } else if (this.interpolationData != null) {
            if (!RenderSystem.isOnRenderThread()) {
                RenderSystem.recordRenderCall(this::updateInterpolatedTexture);
            } else {
                this.updateInterpolatedTexture();
            }
        }

        this.active = false;
    }

    private void updateInterpolatedTexture() {
        this.interpolationData.uploadInterpolatedFrame();
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
