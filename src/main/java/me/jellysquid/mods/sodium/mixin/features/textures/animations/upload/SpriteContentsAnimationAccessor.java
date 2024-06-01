package me.jellysquid.mods.sodium.mixin.features.textures.animations.upload;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(TextureAtlasSprite.AnimatedTexture.class)
public interface SpriteContentsAnimationAccessor {
    @Accessor
    List<TextureAtlasSprite.FrameInfo> getFrames();

    @Accessor
    int getFrameRowSize();
}
