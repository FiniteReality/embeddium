package me.jellysquid.mods.sodium.mixin.features.texture_updates;

import me.jellysquid.mods.sodium.client.util.color.ColorMixer;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Sprite.Interpolation.class)
public class MixinSpriteInterpolated {
    @Shadow
    @Final
    private NativeImage[] images;

    @Unique
    private Sprite parent;

    private static final int STRIDE = 4;

    /**
     * @author IMS
     * @reason Replace fragile Shadow
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    public void assignParent(Sprite parent, Sprite.Info info, int maxLevel, CallbackInfo ci) {
        this.parent = parent;
    }

    /**
     * @author JellySquid
     * @reason Drastic optimizations
     */
    @Overwrite
    void apply(Sprite.Animation animation) {
        Sprite.AnimationFrame animationFrame = animation.frames.get(animation.frameIndex);

        int curIndex = animationFrame.index;
        int nextIndex = animation.frames.get((animation.frameIndex + 1) % animation.frames.size()).index;

        if (curIndex == nextIndex) {
            return;
        }

        float delta = 1.0F - (float) animation.frameTicks / (float) animationFrame.time;

        int f1 = ColorMixer.getStartRatio(delta);
        int f2 = ColorMixer.getEndRatio(delta);

        for (int layer = 0; layer < this.images.length; layer++) {
            int width = this.parent.width >> layer;
            int height = this.parent.height >> layer;

            int curX = ((curIndex % animation.frameCount) * width);
            int curY = ((curIndex / animation.frameCount) * height);

            int nextX = ((nextIndex % animation.frameCount) * width);
            int nextY = ((nextIndex / animation.frameCount) * height);

            NativeImage src = this.parent.images[layer];
            NativeImage dst = this.images[layer];

            // Destination pointers
            long dp = dst.pointer;

            for (int layerY = 0; layerY < height; layerY++) {
                // Source pointers
                long s1p = src.pointer + (curX + ((long) (curY + layerY) * src.getWidth())) * STRIDE;
                long s2p = src.pointer + (nextX + ((long) (nextY + layerY) * src.getWidth())) * STRIDE;

                for (int layerX = 0; layerX < width; layerX++) {
                    int colorA = MemoryUtil.memGetInt(s1p);
                    int colorB = MemoryUtil.memGetInt(s2p);
                    int colorMixed = ColorMixer.mixARGB(colorA, colorB, f1, f2) & 0x00FFFFFF;
                    // Use alpha from first color as-is, do not blend
                    MemoryUtil.memPutInt(dp, colorMixed | (colorA & 0xFF000000));

                    s1p += STRIDE;
                    s2p += STRIDE;
                    dp += STRIDE;
                }
            }
        }

        this.parent.upload(0, 0, this.images);
    }

}
