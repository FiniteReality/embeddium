package me.jellysquid.mods.sodium.client.gl.array;

import me.jellysquid.mods.sodium.client.gl.GlObject;

import com.mojang.blaze3d.platform.GlStateManager;

/**
 * Provides Vertex Array functionality on supported platforms.
 */
public class GlVertexArray extends GlObject {
    public static final int NULL_ARRAY_ID = 0;

    public GlVertexArray() {
        this.setHandle(GlStateManager._glGenVertexArrays());
    }
}
