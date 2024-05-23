package org.embeddedt.embeddium.api.render.chunk;

import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.level.block.entity.BlockEntity;

public interface SectionInfoBuilder {
    /**
     * Adds a sprite to this data container for tracking. If the sprite is tickable, it will be ticked every frame
     * before rendering as necessary.
     *
     * @param sprite The sprite
     */
    void addSprite(TextureAtlasSprite sprite);

    /**
     * Adds a block entity to the data container.
     *
     * @param entity The block entity itself
     * @param cull   True if the block entity can be culled to this chunk render's volume, otherwise false
     */
    void addBlockEntity(BlockEntity entity, boolean cull);
}
