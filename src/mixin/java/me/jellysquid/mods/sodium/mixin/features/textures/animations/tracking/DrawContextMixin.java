package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiComponent.class)
public class DrawContextMixin {
    @Inject(method = "blit(Lcom/mojang/blaze3d/vertex/PoseStack;IIIIILnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("HEAD"))
    private static void preDrawSprite(PoseStack stack, int x, int y, int z,
                               int width, int height,
                               TextureAtlasSprite sprite,
                               CallbackInfo ci)
    {
        SpriteUtil.markSpriteActive(sprite);
    }
}
