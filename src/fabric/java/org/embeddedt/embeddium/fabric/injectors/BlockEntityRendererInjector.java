package org.embeddedt.embeddium.fabric.injectors;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;

public interface BlockEntityRendererInjector {
    default AABB getRenderBoundingBox(BlockEntity entity) {
        return AABBInjector.INFINITE;
    }
}
