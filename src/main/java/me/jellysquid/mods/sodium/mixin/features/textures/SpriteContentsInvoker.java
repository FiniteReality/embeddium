package me.jellysquid.mods.sodium.mixin.features.textures;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(TextureAtlasSprite.class)
public interface SpriteContentsInvoker {
    @Invoker
    void invokeUpload(int x, int y, NativeImage[] images);
}