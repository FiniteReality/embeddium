package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.phys.AABB;
import org.embeddedt.embeddium.fabric.injectors.AABBInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(AABB.class)
public class AABBMixin implements AABBInjector {
}
