package me.jellysquid.mods.sodium.mixin.features.textures.animations.upload;

import me.jellysquid.mods.sodium.client.util.NativeImageHelper;
import me.jellysquid.mods.sodium.mixin.features.textures.SpriteContentsInvoker;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.platform.NativeImage;

import java.util.List;

@Mixin(TextureAtlasSprite.InterpolationData.class)
public class SpriteContentsInterpolationMixin {
    @Shadow
    @Final
    private NativeImage[] activeFrame;

    @Unique
    private TextureAtlasSprite parent;

    @Unique
    private static final int STRIDE = 4;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(TextureAtlasSprite parent, TextureAtlasSprite.Info pSpriteInfo, int pMipLevel, CallbackInfo ci) {
        this.parent = parent;
    }

    /**
     * @author JellySquid
     * @reason Drastic optimizations
     */
    @Overwrite
    void uploadInterpolatedFrame(TextureAtlasSprite.AnimatedTexture animation) {
        TextureAtlasSprite.FrameInfo animationFrame = animation.frames.get(animation.frame);

        int curIndex = animationFrame.index;
        int nextIndex = animation.frames.get((animation.frame + 1) % animation.frames.size()).index;

        if (curIndex == nextIndex) {
            return;
        }

        float delta = 1.0F - (float) animation.subFrame / (float) animationFrame.time;

        for (int layer = 0; layer < this.activeFrame.length; layer++) {
            int width = this.parent.width >> layer;
            int height = this.parent.height >> layer;

            int curX = ((curIndex % animation.frameRowSize) * width);
            int curY = ((curIndex / animation.frameRowSize) * height);

            int nextX = ((nextIndex % animation.frameRowSize) * width);
            int nextY = ((nextIndex / animation.frameRowSize) * height);

            NativeImage src = this.parent.mainImage[layer];
            NativeImage dst = this.activeFrame[layer];

            // Destination pointers
            long dp = dst.pixels;

            for (int layerY = 0; layerY < height; layerY++) {
                // Source pointers
                long s1p = src.pixels + (curX + ((long) (curY + layerY) * src.getWidth())) * STRIDE;
                long s2p = src.pixels + (nextX + ((long) (nextY + layerY) * src.getWidth())) * STRIDE;

                for (int layerX = 0; layerX < width; layerX++) {
                    int colorA = MemoryUtil.memGetInt(s1p);
                    int colorB = MemoryUtil.memGetInt(s2p);
                    int colorMixed = ColorMixer.mix(colorA, colorB, delta) & 0x00FFFFFF;
                    // Use alpha from first color as-is, do not blend
                    MemoryUtil.memPutInt(dp, colorMixed | (colorA & 0xFF000000));

                    s1p += STRIDE;
                    s2p += STRIDE;
                    dp += STRIDE;
                }
            }
        }

        this.parent.upload(0, 0, this.activeFrame);
    }
}
