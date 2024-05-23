package org.embeddedt.embeddium.render.viewport.frustum;

public interface Frustum {
    boolean testAab(float minX, float minY, float minZ, float maxX, float maxY, float maxZ);
}
