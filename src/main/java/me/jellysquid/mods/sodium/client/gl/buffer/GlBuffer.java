package me.jellysquid.mods.sodium.client.gl.buffer;

import me.jellysquid.mods.sodium.client.gl.GlObject;

import com.mojang.blaze3d.platform.GlStateManager;

public abstract class GlBuffer extends GlObject {
    public static final int NULL_BUFFER_ID = 0;

    private GlBufferMapping activeMapping;

    protected GlBuffer() {
        this.setHandle(GlStateManager._glGenBuffers());
    }

    public GlBufferMapping getActiveMapping() {
        return this.activeMapping;
    }

    public void setActiveMapping(GlBufferMapping mapping) {
        this.activeMapping = mapping;
    }
}
