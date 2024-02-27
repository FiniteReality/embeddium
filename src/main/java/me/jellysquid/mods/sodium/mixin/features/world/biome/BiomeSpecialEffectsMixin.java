package me.jellysquid.mods.sodium.mixin.features.world.biome;

import net.minecraft.world.level.biome.BiomeSpecialEffects;
import org.embeddedt.embeddium.chunk.biome.ExtendedBiomeSpecialEffects;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

@Mixin(BiomeSpecialEffects.class)
public abstract class BiomeSpecialEffectsMixin implements ExtendedBiomeSpecialEffects {
    @Shadow
    public abstract Optional<Integer> getGrassColorOverride();

    @Shadow
    public abstract Optional<Integer> getFoliageColorOverride();

    @Unique
    private boolean hasCustomGrassColor;

    @Unique
    private int customGrassColor;

    @Unique
    private boolean hasCustomFoliageColor;

    @Unique
    private int customFoliageColor;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void cacheColorInfo(CallbackInfo ci) {
        var grassColor = this.getGrassColorOverride();

        if (grassColor.isPresent()) {
            this.hasCustomGrassColor = true;
            this.customGrassColor = grassColor.get();
        }

        var foliageColor = this.getFoliageColorOverride();

        if (foliageColor.isPresent()) {
            this.hasCustomFoliageColor = true;
            this.customFoliageColor = foliageColor.get();
        }
    }

    @Override
    public boolean embeddium$hasCustomFoliage() {
        return this.hasCustomFoliageColor;
    }

    @Override
    public boolean embeddium$hasCustomGrass() {
        return this.hasCustomGrassColor;
    }

    @Override
    public int embeddium$getCustomFoliage() {
        return this.customFoliageColor;
    }

    @Override
    public int embeddium$getCustomGrass() {
        return this.customGrassColor;
    }
}
