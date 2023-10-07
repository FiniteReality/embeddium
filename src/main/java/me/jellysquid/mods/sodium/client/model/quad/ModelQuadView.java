package me.jellysquid.mods.sodium.client.model.quad;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import net.minecraft.client.texture.Sprite;

/**
 * Provides a read-only view of a model quad. For mutable access to a model quad, see {@link ModelQuadViewMutable}.
 */
public interface ModelQuadView {
    /**
     * @return The x-position of the vertex at index {@param idx}
     */
    float getX(int idx);

    /**
     * @return The y-position of the vertex at index {@param idx}
     */
    float getY(int idx);

    /**
     * @return The z-position of the vertex at index {@param idx}
     */
    float getZ(int idx);

    /**
     * @return The integer-encoded color of the vertex at index {@param idx}
     */
    int getColor(int idx);

    /**
     * @return The texture x-coordinate for the vertex at index {@param idx}
     */
    float getTexU(int idx);

    /**
     * @return The texture y-coordinate for the vertex at index {@param idx}
     */
    float getTexV(int idx);

    /**
     * @return The packed lightmap coordinates for the vertex at index {@param idx},
     * or zero if regular lighting should be used.
     */
    int getLight(int idx);

    /**
     * @return The integer bit flags containing the {@link ModelQuadFlags} for this quad
     */
    int getFlags();

    /**
     * @return The color index of this quad.
     */
    int getColorIndex();

    /**
     * @return The sprite texture used by this quad, or null if none is attached
     */
    Sprite getSprite();

    /**
     * @return The normal vector of the quad in 3-byte packed format
     */
    int getNormal();

    int getForgeNormal(int idx);

    default boolean hasColor() {
        return this.getColorIndex() != -1;
    }

    boolean hasAmbientOcclusion();
}
