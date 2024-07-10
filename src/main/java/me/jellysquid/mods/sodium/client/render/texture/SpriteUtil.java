package me.jellysquid.mods.sodium.client.render.texture;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;

/**
 * @deprecated use {@link org.embeddedt.embeddium.api.render.texture.SpriteUtil} instead
 */
@Deprecated
public class SpriteUtil {
    public static void markSpriteActive(TextureAtlasSprite sprite) {
        org.embeddedt.embeddium.api.render.texture.SpriteUtil.markSpriteActive(sprite);
    }
}
