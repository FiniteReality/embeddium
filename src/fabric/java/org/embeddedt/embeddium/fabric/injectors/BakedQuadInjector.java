package org.embeddedt.embeddium.fabric.injectors;

public interface BakedQuadInjector {
    default boolean hasAmbientOcclusion() {
        return true;
    }
}
