package org.embeddedt.embeddium.render.fluid;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;

public class EmbeddiumFluidSpriteCache {
    // Cache the sprites array to avoid reallocating it on every call
    private final TextureAtlasSprite[] sprites = new TextureAtlasSprite[3];
    private final Object2ObjectOpenHashMap<ResourceLocation, TextureAtlasSprite> spriteCache = new Object2ObjectOpenHashMap<>();

    private TextureAtlasSprite getTexture(ResourceLocation identifier) {
        TextureAtlasSprite sprite = spriteCache.get(identifier);

        if (sprite == null) {
            sprite = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(identifier);
            spriteCache.put(identifier, sprite);
        }

        return sprite;
    }

    public TextureAtlasSprite[] getSprites(BlockAndTintGetter world, BlockPos pos, FluidState fluidState) {
        IClientFluidTypeExtensions fluidExt = IClientFluidTypeExtensions.of(fluidState);
        sprites[0] = getTexture(fluidExt.getStillTexture(fluidState, world, pos));
        sprites[1] = getTexture(fluidExt.getFlowingTexture(fluidState, world, pos));
        ResourceLocation overlay = fluidExt.getOverlayTexture(fluidState, world, pos);
        sprites[2] = overlay != null ? getTexture(overlay) : null;
        return sprites;
    }
}
