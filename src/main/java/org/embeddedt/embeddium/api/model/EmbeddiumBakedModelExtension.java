package org.embeddedt.embeddium.api.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;

public interface EmbeddiumBakedModelExtension {
    default boolean useAmbientOcclusionWithLightEmission(BlockState state, RenderType renderType) {
        return false;
    }
}
