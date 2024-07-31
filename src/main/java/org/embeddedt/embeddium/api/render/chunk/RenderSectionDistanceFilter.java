package org.embeddedt.embeddium.api.render.chunk;

@FunctionalInterface
public interface RenderSectionDistanceFilter {
    /**
     * The default filter mimics vanilla's "cylindrical fog" algorithm.
     * max(length(distance.xz), abs(distance.y))
     */
    RenderSectionDistanceFilter DEFAULT = (dx, dy, dz, maxDistance) -> ((((dx * dx) + (dz * dz)) < (maxDistance * maxDistance)) && (Math.abs(dy) < maxDistance));

    boolean isWithinDistance(float xDistance, float yDistance, float zDistance, float maxDistance);
}
