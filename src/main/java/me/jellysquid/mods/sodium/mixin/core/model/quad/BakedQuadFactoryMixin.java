package me.jellysquid.mods.sodium.mixin.core.model.quad;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import me.jellysquid.mods.sodium.client.model.quad.BakedQuadView;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockElementFace;
import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
            float[] uvs = face.uv.uvs;
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

    /**
     * Backport of NeoForge PR <a href="https://github.com/neoforged/NeoForge/pull/207">#207</a>. The Forge patch
     * here reduces UV precision for no reason and has not been needed since at least 1.14. Vanilla already
     * adjusts the UV offsets itself.
     */
    @Inject(method = "fillVertex([IILorg/joml/Vector3f;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;Lnet/minecraft/client/renderer/block/model/BlockFaceUV;)V", at = @At("RETURN"))
    private void undoForgeUVExpansion(int[] vertices, int cornerIndex, Vector3f position, TextureAtlasSprite sprite, BlockFaceUV element, CallbackInfo ci) {
        int i = cornerIndex * 8;
        vertices[i + 4] = Float.floatToRawIntBits(sprite.getU(element.getU(cornerIndex)));
        vertices[i + 4 + 1] = Float.floatToRawIntBits(sprite.getV(element.getV(cornerIndex)));
    }
}
