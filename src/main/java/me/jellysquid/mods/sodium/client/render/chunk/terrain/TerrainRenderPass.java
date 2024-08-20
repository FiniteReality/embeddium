package me.jellysquid.mods.sodium.client.render.chunk.terrain;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.client.renderer.RenderType;

/**
 * A terrain render pass corresponds to a draw call to render some subset of terrain geometry. Passes are generally
 * used for fixed configuration that will not change from quad to quad and allow for optimizations to be made
 * within the terrain shader code at compile time (e.g. omitting the fragment discard conditional entirely on the solid pass).
 * <p></p>
 * Geometry that shares the same terrain render pass may still be able to specify some more dynamic properties. See {@link Material}
 * for more information.
 */
public class TerrainRenderPass {
    @Deprecated(forRemoval = true)
    private final RenderType layer;

    private final boolean useReverseOrder;
    private final boolean fragmentDiscard;

    /**
     * Constructs a new terrain render pass. The provided RenderType is only used to set up/clear GPU pipeline state,
     * and otherwise has no special meaning to the renderer.
     * <p>
     * Refer to the public getters for documentation on the other parameters.
     */
    public TerrainRenderPass(RenderType layer, boolean useReverseOrder, boolean allowFragmentDiscard) {
        this.layer = layer;

        this.useReverseOrder = useReverseOrder;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    /**
     * {@return whether sections on this render pass should be rendered farthest-to-nearest, rather than nearest-to-farthest}
     */
    public boolean isReverseOrder() {
        return this.useReverseOrder;
    }

    /**
     * {@return whether this render pass wants to opt in to translucency sorting}
     */
    public boolean isSorted() {
        return this.useReverseOrder && SodiumClientMod.canApplyTranslucencySorting();
    }

    @Deprecated
    public void startDrawing() {
        this.layer.setupRenderState();
    }

    @Deprecated
    public void endDrawing() {
        this.layer.clearRenderState();
    }

    /**
     * {@return whether the fragment discard check should be enabled for this render pass}
     */
    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }
}
