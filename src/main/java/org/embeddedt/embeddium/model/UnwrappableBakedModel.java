package org.embeddedt.embeddium.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

/**
 * Represents a model that may be "unwrapped" given sufficient context.
 */
public interface UnwrappableBakedModel {
    /**
     * Return the "inner model" of this model. Note that any calls made to the inner model will not have the RandomSource
     * adjusted to accommodate any changes the wrapping model would normally be making to it. As such, you should
     * only return a non-null value if the RandomSource is not used by the inner model.
     */
    @Nullable BakedModel embeddium$getInnerModel(RandomSource rand);

    static BakedModel unwrapIfPossible(BakedModel incoming, RandomSource rand) {
        if(incoming instanceof UnwrappableBakedModel) {
            BakedModel m = ((UnwrappableBakedModel)incoming).embeddium$getInnerModel(rand);
            if(m != null) {
                return m;
            }
        }
        return incoming;
    }
}
