package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.world.level.Level;
import org.embeddedt.embeddium.fabric.injectors.LevelInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(Level.class)
public class LevelMixin implements LevelInjector {
}
