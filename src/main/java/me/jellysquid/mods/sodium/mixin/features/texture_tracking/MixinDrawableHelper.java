package me.jellysquid.mods.sodium.mixin.features.texture_tracking;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(GuiComponent.class)
public class MixinDrawableHelper {
    @Inject(method = "blit", at = @At("HEAD"))
    private static void onHeadDrawSprite(PoseStack matrices, int x, int y, int z, int width, int height, TextureAtlasSprite sprite, CallbackInfo ci) {
        SpriteUtil.markSpriteActive(sprite);
    }
}
