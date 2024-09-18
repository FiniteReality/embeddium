package me.jellysquid.mods.sodium.mixin.features.world.biome;

import me.jellysquid.mods.sodium.client.world.biome.BiomeColorMaps;
import net.minecraft.util.Mth;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = Biome.class, priority = 800)
public abstract class BiomeMixin {
    @Shadow
    public abstract float getTemperature();

    @Shadow
    public abstract float getDownfall();

    @Unique
    private int defaultColorIndex;

    @Inject(method = "/<init>/", at = @At("RETURN"))
    private void setupColors(CallbackInfo ci) {
        this.defaultColorIndex = this.getDefaultColorIndex();
    }

    /**
     * @author JellySquid
     * @reason Avoid unnecessary allocations
     */
    @Overwrite
    public int getGrassColor(double x, double z) {
        return BiomeColorMaps.getGrassColor(this.defaultColorIndex);
    }

    /**
     * @author JellySquid
     * @reason Avoid allocations
     */
    @Overwrite
    public int getFoliageColor() {
        return BiomeColorMaps.getFoliageColor(this.defaultColorIndex);
    }

    @Unique
    private int getDefaultColorIndex() {
        double temperature = Mth.clamp(this.getTemperature(), 0.0F, 1.0F);
        double humidity = Mth.clamp(this.getDownfall(), 0.0F, 1.0F);

        return BiomeColorMaps.getIndex(temperature, humidity);
    }
}
