package me.jellysquid.mods.sodium.mixin.features.world.biome;

import net.minecraft.world.level.FoliageColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(FoliageColor.class)
public interface FoliageColorsAccessor {
    @Accessor("pixels")
    static int[] getColorMap() {
        throw new AssertionError();
    }
}