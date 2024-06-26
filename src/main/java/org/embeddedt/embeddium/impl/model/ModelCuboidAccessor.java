package org.embeddedt.embeddium.impl.model;

import org.embeddedt.embeddium.impl.render.immediate.model.ModelCuboid;
import org.jetbrains.annotations.Nullable;

public interface ModelCuboidAccessor {
    ModelCuboid sodium$copy();

    @Nullable
    ModelCuboid embeddium$getSimpleCuboid();
}
