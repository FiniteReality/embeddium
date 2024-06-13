package org.embeddedt.embeddium.fabric.injectors;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.BakedQuad;

public interface VertexConsumerInjector {
    default void putBulkData(PoseStack.Pose matrices, BakedQuad bakedQuad, float r, float g, float b, float a, int light, int overlay, boolean colorize) {
        throw new AssertionError();
    }
}
