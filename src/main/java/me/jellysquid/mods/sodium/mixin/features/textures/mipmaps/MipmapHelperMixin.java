package me.jellysquid.mods.sodium.mixin.features.textures.mipmaps;

import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mojang.blaze3d.platform.NativeImage;
import me.jellysquid.mods.sodium.client.util.NativeImageHelper;
import me.jellysquid.mods.sodium.client.util.color.ColorSRGB;
import net.minecraft.client.renderer.texture.MipmapGenerator;
import net.minecraft.util.FastColor;
import org.lwjgl.system.MemoryUtil;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.OptionalInt;

/**
 * Implements a significantly enhanced mipmap downsampling filter.
 *
 * <p>This algorithm combines:
 * <li>
 *     <ul>ideas from vanilla Minecraft -- using linear color spaces instead of sRGB for blending)</ul>
 *     <ul>ideas from OptiFine -- using the alpha values for weighting in downsampling</ul>
 *     <ul>ideas from Sodium -- replacing the transparent black pixels in textures with the weighted average color of non-transparent pixels</ul>
 * </li>
 * to produce a novel downsampling
 * algorithm for mipmapping that produces minimal visual artifacts.</p>
 *
 * <p>This implementation fixes a number of issues with other implementations:</p>
 *
 * <li>
 *     <ul>OptiFine blends in sRGB space, resulting in brightness losses.</ul>
 *     <ul>Vanilla applies gamma correction to alpha values, which has weird results when alpha values aren't the same.</ul>
 *     <ul>Vanilla computes a simple average of the 4 pixels, disregarding the relative alpha values of pixels. In
 *         cutout textures, this results in a lot of pixels with high alpha values and dark colors, causing visual
 *         artifacts.</ul>
 *     <ul>Sodium replaces the transparent black pixels on the original texture (not just when mipmapping), which causes leaves to look
 *     bad in Fast graphics without a texture replacement. </ul>
 * </li>
 *
 * This mixin is based on original work from Iris at <a href="https://github.com/IrisShaders/Iris/blob/41095ac23ea0add664afd1b85c414d1f1ed94066/src/main/java/net/coderbot/iris/mixin/bettermipmaps/MixinMipmapGenerator.java">MixinMipmapGenerator</a>.
 */
@Mixin(MipmapGenerator.class)
public class MipmapHelperMixin {
    /**
     * @author coderbot
     * @reason replace the vanilla blending function with our improved function
     */
    @Overwrite
    private static int alphaBlend(int one, int two, int three, int four, boolean checkAlpha) {
        // First blend horizontally, then blend vertically.
        //
        // This works well for the case where our change is the most impactful (grass side overlays)
        return weightedAverageColor(weightedAverageColor(one, two), weightedAverageColor(three, four));
    }

    /**
     * @author embeddedt
     * @reason Compute the average color of this texture while checking if it contains transparent pixels.
     */
    @Redirect(method = "generateMipLevels", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/MipmapGenerator;hasTransparentPixel(Lcom/mojang/blaze3d/platform/NativeImage;)Z"))
    private static boolean storeAverageColorIfTransparent(NativeImage image, @Share("averageColor") LocalRef<OptionalInt> averageColorRef) {
        var averageColor = sodium$computeAveragePixelColor(image);
        averageColorRef.set(averageColor);
        return averageColor.isPresent();
    }

    /**
     * @author embeddedt
     * @reason When retrieving pixels from the original texture (level 0), check if the pixel has alpha 0,
     * and if so, use the average color in its place.
     */
    @Redirect(method = "generateMipLevels", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/platform/NativeImage;getPixelRGBA(II)I"))
    private static int getPixelOrTransparent(NativeImage image, int x, int y, NativeImage[] imageArray, @Share("averageColor") LocalRef<OptionalInt> averageColorRef) {
        int originalPixel = image.getPixelRGBA(x, y);
        if(image == imageArray[0] && FastColor.ABGR32.alpha(originalPixel) == 0) {
            return averageColorRef.get().orElse(originalPixel);
        } else {
            return originalPixel;
        }
    }

    /**
     * Compute the average pixel color of all non-transparent pixels in nativeImage. If the image
     * doesn't contain any fully transparent pixels, returns an empty optional int.
     * @param nativeImage the image to read pixels from
     * @return the average color, or empty if there are no fully transparent pixels
     */
    @Unique
    private static OptionalInt sodium$computeAveragePixelColor(NativeImage nativeImage) {
        final long ppPixel = NativeImageHelper.getPointerRGBA(nativeImage);
        final int pixelCount = nativeImage.getHeight() * nativeImage.getWidth();

        // Calculate an average color from all pixels that are not completely transparent.
        // This average is weighted based on the (non-zero) alpha value of the pixel.
        float r = 0.0f;
        float g = 0.0f;
        float b = 0.0f;

        float totalWeight = 0.0f;

        for (int pixelIndex = 0; pixelIndex < pixelCount; pixelIndex++) {
            long pPixel = ppPixel + (pixelIndex * 4);

            int color = MemoryUtil.memGetInt(pPixel);
            int alpha = FastColor.ABGR32.alpha(color);

            // Ignore all fully-transparent pixels for the purposes of computing an average color.
            if (alpha != 0) {
                float weight = (float) alpha;

                // Make sure to convert to linear space so that we don't lose brightness.
                r += ColorSRGB.srgbToLinear(FastColor.ABGR32.red(color)) * weight;
                g += ColorSRGB.srgbToLinear(FastColor.ABGR32.green(color)) * weight;
                b += ColorSRGB.srgbToLinear(FastColor.ABGR32.blue(color)) * weight;

                totalWeight += weight;
            }
        }

        // Bail if none of the pixels are semi-transparent.
        if (totalWeight == 0.0f) {
            return OptionalInt.empty();
        }

        r /= totalWeight;
        g /= totalWeight;
        b /= totalWeight;

        // Convert that color in linear space back to sRGB.
        // Use an alpha value of zero - this works since we only replace pixels with an alpha value of 0.
        return OptionalInt.of(ColorSRGB.linearToSrgb(r, g, b, 0));
    }

    @Unique
    private static int weightedAverageColor(int one, int two) {
        int alphaOne = FastColor.ABGR32.alpha(one);
        int alphaTwo = FastColor.ABGR32.alpha(two);

        // In the case where the alpha values of the same, we can get by with an unweighted average.
        if (alphaOne == alphaTwo) {
            return averageRgb(one, two, alphaOne);
        }

        // If one of our pixels is fully transparent, ignore it.
        // We just take the value of the other pixel as-is. To compensate for not changing the color value, we
        // divide the alpha value by 4 instead of 2.
        if (alphaOne == 0) {
            return (two & 0x00FFFFFF) | ((alphaTwo >> 2) << 24);
        }

        if (alphaTwo == 0) {
            return (one & 0x00FFFFFF) | ((alphaOne >> 2) << 24);
        }

        // Use the alpha values to compute relative weights of each color.
        float scale = 1.0f / (alphaOne + alphaTwo);

        float relativeWeightOne = alphaOne * scale;
        float relativeWeightTwo = alphaTwo * scale;

        // Convert the color components into linear space, then multiply the corresponding weight.
        float oneR = ColorSRGB.srgbToLinear(FastColor.ABGR32.red(one)) * relativeWeightOne;
        float oneG = ColorSRGB.srgbToLinear(FastColor.ABGR32.green(one)) * relativeWeightOne;
        float oneB = ColorSRGB.srgbToLinear(FastColor.ABGR32.blue(one)) * relativeWeightOne;

        float twoR = ColorSRGB.srgbToLinear(FastColor.ABGR32.red(two)) * relativeWeightTwo;
        float twoG = ColorSRGB.srgbToLinear(FastColor.ABGR32.green(two)) * relativeWeightTwo;
        float twoB = ColorSRGB.srgbToLinear(FastColor.ABGR32.blue(two)) * relativeWeightTwo;

        // Combine the color components of each color
        float linearR = oneR + twoR;
        float linearG = oneG + twoG;
        float linearB = oneB + twoB;

        // Take the average alpha of both alpha values
        int averageAlpha = (alphaOne + alphaTwo) >> 1;

        // Convert to sRGB and pack the colors back into an integer.
        return ColorSRGB.linearToSrgb(linearR, linearG, linearB, averageAlpha);
    }

    // Computes a non-weighted average of the two sRGB colors in linear space, avoiding brightness losses.
    @Unique
    private static int averageRgb(int a, int b, int alpha) {
        float ar = ColorSRGB.srgbToLinear(FastColor.ABGR32.red(a));
        float ag = ColorSRGB.srgbToLinear(FastColor.ABGR32.green(a));
        float ab = ColorSRGB.srgbToLinear(FastColor.ABGR32.blue(a));

        float br = ColorSRGB.srgbToLinear(FastColor.ABGR32.red(b));
        float bg = ColorSRGB.srgbToLinear(FastColor.ABGR32.green(b));
        float bb = ColorSRGB.srgbToLinear(FastColor.ABGR32.blue(b));

        return ColorSRGB.linearToSrgb((ar + br) * 0.5f, (ag + bg) * 0.5f, (ab + bb) * 0.5f, alpha);
    }
}
