package me.jellysquid.mods.sodium.mixin.features.world.biome;

import net.minecraft.world.level.GrassColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GrassColor.class)
public interface GrassColorsAccessor {
    @Accessor("pixels")
    static int[] getColorMap() {
        throw new AssertionError();
    }
}
