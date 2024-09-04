package me.jellysquid.mods.sodium.client.render.chunk.terrain;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.experimental.Accessors;
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
@Builder
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@Accessors(fluent = true)
public class TerrainRenderPass {
    /**
     * The RenderType that is used to set up/clear GPU pipeline state.
     */
    private final RenderType layer;

    /**
     * Whether sections on this render pass should be rendered farthest-to-nearest, rather than nearest-to-farthest.
     */
    private final boolean useReverseOrder;
    /**
     * Whether fragment alpha testing should be enabled for this render pass.
     */
    private final boolean fragmentDiscard;
    /**
     * Whether this render pass wants to opt in to translucency sorting if enabled.
     */
    private final boolean useTranslucencySorting;

    @Deprecated
    public TerrainRenderPass(RenderType layer, boolean useReverseOrder, boolean allowFragmentDiscard) {
        this.layer = layer;

        this.useReverseOrder = useReverseOrder;
        this.fragmentDiscard = allowFragmentDiscard;
        this.useTranslucencySorting = useReverseOrder;
    }


    public boolean isReverseOrder() {
        return this.useReverseOrder;
    }


    public boolean isSorted() {
        return this.useTranslucencySorting && SodiumClientMod.canApplyTranslucencySorting();
    }

    @Deprecated
    public void startDrawing() {
        this.layer.setupRenderState();
    }

    @Deprecated
    public void endDrawing() {
        this.layer.clearRenderState();
    }


    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }
}
