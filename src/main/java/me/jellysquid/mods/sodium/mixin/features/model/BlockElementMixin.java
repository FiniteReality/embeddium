package me.jellysquid.mods.sodium.mixin.features.model;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.renderer.block.model.BlockElement;
import org.embeddedt.embeddium.model.EpsilonizableBlockElement;
import org.embeddedt.embeddium.util.PlatformUtil;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(BlockElement.class)
public class BlockElementMixin implements EpsilonizableBlockElement {
    @Shadow
    @Final
    public Vector3f from;
    @Shadow
    @Final
    public Vector3f to;

    private boolean embeddium$hasEpsilonized;

    @Override
    public synchronized void embeddium$epsilonize() {
        if(!embeddium$hasEpsilonized) {
            embeddium$hasEpsilonized = true;
            if (!PlatformUtil.isLoadValid() || !SodiumClientMod.options().performance.useCompactVertexFormat) {
                return;
            }
            embeddium$epsilonize(from);
            embeddium$epsilonize(to);
        }
    }

    private static void embeddium$epsilonize(Vector3f v) {
        v.x = embeddium$epsilonize(v.x);
        v.y = embeddium$epsilonize(v.y);
        v.z = embeddium$epsilonize(v.z);
    }

    private static final float EMBEDDIUM$MINIMUM_EPSILON = 0.008f;

    private static float embeddium$epsilonize(float f) {
        int roundedCoord = Math.round(f);
        float difference = f - roundedCoord;
        // Ignore components that are integers or far enough away from a texel for epsilonizing to be unnecessary
        if(difference == 0 || Math.abs(difference) >= EMBEDDIUM$MINIMUM_EPSILON) {
            return f;
        } else {
            // "Push" the coordinate slightly further away from the texel so z-fighting is less likely
            // This maps e.g. 0.001 -> 0.01, which should be enough
            return roundedCoord + (difference * 10);
        }
    }
}
