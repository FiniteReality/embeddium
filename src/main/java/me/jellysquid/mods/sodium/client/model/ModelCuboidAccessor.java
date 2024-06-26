package me.jellysquid.mods.sodium.client.model;

import me.jellysquid.mods.sodium.client.render.immediate.model.ModelCuboid;
import org.jetbrains.annotations.Nullable;

public interface ModelCuboidAccessor {
    ModelCuboid sodium$copy();

    @Nullable
    ModelCuboid embeddium$getSimpleCuboid();
}
