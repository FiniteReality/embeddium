package me.jellysquid.mods.sodium.client.model.color;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.core.BlockPos;

/**
 * Custom interface for generating color data for quads. Compared to vanilla's BlockColor interface, ColorProvider
 * provides access to the quad geometry (not just the tint index) and retrieves the colors for all four vertices
 * at once. This enables more flexibility in how the colors are generated (e.g. blending based on vertex positions
 * rather than choosing a solid color for the whole face) and reduces overhead.
 */
public interface ColorProvider<T> {
    /**
     * Computes the per-vertex colors of a model quad and stores the results in {@param output}. The order of
     * the output color array is the same as the order of the quad's vertices.
     *
     * @param view   The world which contains the object being colorized
     * @param pos    The position of the object being colorized
     * @param state  The state of the object being colorized
     * @param quad   The quad geometry which should be colorized
     * @param output The output array of vertex colors (in ABGR format)
     */
    void getColors(WorldSlice view, BlockPos pos, T state, ModelQuadView quad, int[] output);
}
