package org.embeddedt.embeddium.impl.mixin.features.textures.animations.tracking;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

@Mixin(GuiGraphics.class)
public class DrawContextMixin {
    @Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIIIIIII)V", at = @At("HEAD"))
    private void preDrawSprite(Function<ResourceLocation, RenderType> function, TextureAtlasSprite sprite, int i, int j, int k, int l, int m, int n, int o, int p, int q, CallbackInfo ci)
    {
        SpriteUtil.markSpriteActive(sprite);
    }

    @Inject(method = "blitSprite(Ljava/util/function/Function;Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;IIIII)V", at = @At("HEAD"))
    private void preDrawSprite(Function<ResourceLocation, RenderType> function, TextureAtlasSprite sprite, int i, int j, int k, int l, int m, CallbackInfo ci)
    {
        SpriteUtil.markSpriteActive(sprite);
    }
}
