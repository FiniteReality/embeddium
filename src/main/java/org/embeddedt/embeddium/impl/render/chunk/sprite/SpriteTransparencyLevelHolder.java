package org.embeddedt.embeddium.impl.render.chunk.sprite;

import net.minecraft.client.renderer.texture.SpriteContents;

public interface SpriteTransparencyLevelHolder {
    SpriteTransparencyLevel embeddium$getTransparencyLevel();

    static SpriteTransparencyLevel getTransparencyLevel(SpriteContents contents) {
        return ((SpriteTransparencyLevelHolder)contents).embeddium$getTransparencyLevel();
    }
}
