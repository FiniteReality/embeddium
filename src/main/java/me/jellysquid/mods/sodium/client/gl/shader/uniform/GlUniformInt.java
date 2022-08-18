package me.jellysquid.mods.sodium.client.gl.shader.uniform;

import com.mojang.blaze3d.platform.GlStateManager;

public class GlUniformInt extends GlUniform<Integer> {
    public GlUniformInt(int index) {
        super(index);
    }

    @Override
    public void set(Integer value) {
        this.setInt(value);
    }

    public void setInt(int value) {
    	GlStateManager._glUniform1i(this.index, value);
    }
}
