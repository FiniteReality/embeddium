package me.jellysquid.mods.sodium.mixin.core.model.quad;

import net.minecraft.client.renderer.block.model.BlockFaceUV;
import net.minecraft.client.renderer.block.model.FaceBakery;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FaceBakery.class)
public class BakedQuadFactoryMixin {
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
