package org.embeddedt.embeddium.render.matrix_stack;

public interface CachingPoseStack {
    /**
     * Enables or disables caching of matrix entries on the given matrix stack. When enabled, any
     * matrix objects returned become invalid after a call to popPose().
     * @param flag whether caching should be enabled
     */
    void embeddium$setCachingEnabled(boolean flag);
}
