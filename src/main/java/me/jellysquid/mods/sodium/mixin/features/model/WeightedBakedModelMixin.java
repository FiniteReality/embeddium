package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.util.WeighedRandom;
import org.embeddedt.embeddium.model.UnwrappableBakedModel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements UnwrappableBakedModel {

    @Shadow
    @Final
    private int totalWeight;

    @Shadow
    @Final
    private List<WeightedBakedModel.WeightedModel> list;

    @Override
    public @Nullable BakedModel embeddium$getInnerModel(Random random) {
        return ((WeightedBakedModel.WeightedModel) WeighedRandom.getWeightedItem(this.list, Math.abs((int)random.nextLong()) % this.totalWeight)).model;
    }
}
