package me.jellysquid.mods.sodium.client.render.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;

public interface EntityLightSampler<T extends Entity> {
    int bridge$getBlockLight(T entity, BlockPos pos);

    int bridge$getSkyLight(T entity, BlockPos pos);
}
