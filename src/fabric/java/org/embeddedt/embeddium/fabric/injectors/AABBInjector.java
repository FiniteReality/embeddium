package org.embeddedt.embeddium.fabric.injectors;

import net.minecraft.world.phys.AABB;

public interface AABBInjector {
    AABB INFINITE = new AABB(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY);
    default boolean isInfinite() {
        return this == INFINITE;
    }
}