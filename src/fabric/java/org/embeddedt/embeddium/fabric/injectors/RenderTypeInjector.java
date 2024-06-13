package org.embeddedt.embeddium.fabric.injectors;

public interface RenderTypeInjector {
    default int getChunkLayerId() {
        throw new AssertionError();
    }
}
