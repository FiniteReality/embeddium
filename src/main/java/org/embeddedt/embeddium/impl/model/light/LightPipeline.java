package org.embeddedt.embeddium.impl.model.light;

import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Light pipelines allow model quads for any location in the world to be lit regardless of what produced them
 * (blocks, fluids, or block entities).
 */
public interface LightPipeline {
    /**
     * Calculates the light data for a given block model quad, storing the result in {@param out}.
     * @param quad The block model quad
     * @param pos The block position of the model this quad belongs to
     * @param out The data arrays which will store the calculated light data results
     * @param cullFace The cull face of the quad
     * @param lightFace The light face of the quad
     * @param shade True if the block is shaded by ambient occlusion
     */
    void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade);

    /**
     * Reset any cached data for this pipeline.
     */
    default void reset() {

    }
}
