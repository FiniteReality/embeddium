package me.jellysquid.mods.sodium.mixin.core.model;

import net.minecraft.client.render.model.BakedQuadFactory;
import net.minecraft.client.render.model.json.ModelElementTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.math.Vec3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedQuadFactory.class)
public class MixinBakedQuadFactory {
    /**
     * Backport of NeoForge PR <a href="https://github.com/neoforged/NeoForge/pull/207">#207</a>. The Forge patch
     * here reduces UV precision for no reason and has not been needed since at least 1.14. Vanilla already
     * adjusts the UV offsets itself.
     */
    @Inject(method = "packVertexData([IILnet/minecraft/util/math/Vec3f;Lnet/minecraft/client/texture/Sprite;Lnet/minecraft/client/render/model/json/ModelElementTexture;)V", at = @At("RETURN"))
    private void undoForgeUVExpansion(int[] vertices, int cornerIndex, Vec3f position, Sprite sprite, ModelElementTexture element, CallbackInfo ci) {
        int i = cornerIndex * 8;
        vertices[i + 4] = Float.floatToRawIntBits(sprite.getFrameU(element.getU(cornerIndex)));
        vertices[i + 4 + 1] = Float.floatToRawIntBits(sprite.getFrameV(element.getV(cornerIndex)));
    }
}
