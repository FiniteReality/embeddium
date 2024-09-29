package org.embeddedt.embeddium.impl.mixin.core.model.quad;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(FaceBakery.class)
public class BakedQuadFactoryMixin {
    /**
     * @author embeddedt
     * @reason Check if quad's UVs are contained within the sprite's boundaries; if so, mark it as having a trusted sprite
     * (meaning the particle sprite matches the encoded UVs)
     */
    @ModifyReturnValue(method = "bakeQuad", at = @At("RETURN"))
    private BakedQuad setMaterialClassification(BakedQuad quad, @Local(ordinal = 0, argsOnly = true) BlockElementFace face, @Local(ordinal = 0, argsOnly = true) TextureAtlasSprite sprite) {
        if (sprite.getClass() == TextureAtlasSprite.class && sprite.contents().getClass() == SpriteContents.class) {
            float[] uvs = face.uv().uvs;
            float minUV = Float.MAX_VALUE, maxUV = Float.MIN_VALUE;

            for (float uv : uvs) {
                minUV = Math.min(minUV, uv);
                maxUV = Math.max(maxUV, uv);
            }

            if (minUV >= 0 && maxUV <= 16) {
                // Quad UVs do not extend outside texture boundary, we can trust the given sprite
                BakedQuadView view = (BakedQuadView)quad;
                view.setFlags(view.getFlags() | ModelQuadFlags.IS_TRUSTED_SPRITE);
            }

        }

        return quad;
    }
}
