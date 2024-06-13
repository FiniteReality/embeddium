package net.neoforged.neoforge.client.textures;

import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public abstract class UnitTextureAtlasSprite extends TextureAtlasSprite {
    public static final UnitTextureAtlasSprite INSTANCE = null;

    protected UnitTextureAtlasSprite(ResourceLocation p_250211_, SpriteContents p_248526_, int p_248950_, int p_249741_, int p_248672_, int p_248637_) {
        super(p_250211_, p_248526_, p_248950_, p_249741_, p_248672_, p_248637_);
    }
}
