package me.jellysquid.mods.sodium.client.render.chunk.terrain;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.renderer.RenderType;

public class TerrainRenderPass {
    @Deprecated(forRemoval = true)
    private final RenderType layer;

    private final boolean useReverseOrder;
    private final boolean fragmentDiscard;

    public TerrainRenderPass(RenderType layer, boolean useReverseOrder, boolean allowFragmentDiscard) {
        this.layer = layer;

        this.useReverseOrder = useReverseOrder;
        this.fragmentDiscard = allowFragmentDiscard;
    }

    public boolean isReverseOrder() {
        return this.useReverseOrder;
    }

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

    public boolean supportsFragmentDiscard() {
        return this.fragmentDiscard;
    }
}
