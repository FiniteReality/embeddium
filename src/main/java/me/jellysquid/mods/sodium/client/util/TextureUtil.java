package me.jellysquid.mods.sodium.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;

public class TextureUtil {

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static int getLightTextureId() {
        return Minecraft.getInstance().getTextureManager().getTexture(Minecraft.getInstance().gameRenderer.lightTexture().lightTextureLocation).getId();
    }

    /**
     * NOTE: Must be called while a RenderLayer is active.
     */
    public static int getBlockTextureId() {
        return Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS).getId();
    }
}
