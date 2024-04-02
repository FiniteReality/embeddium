package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;
import org.embeddedt.embeddium.model.UnwrappableBakedModel;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.List;
import java.util.Random;

@Mixin(WeightedBakedModel.class)
public class MixinWeightedBakedModel implements UnwrappableBakedModel {
    @Shadow
    @Final
    private List<WeightedBakedModel.WeightedModel> list;

    @Shadow
    @Final
    private int totalWeight;

    /**
     * @author JellySquid
     * @reason Avoid excessive object allocations
     */
    @Overwrite
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, Random random, IModelData modelData) {
    	WeightedBakedModel.WeightedModel entry = getAt(this.list, Math.abs((int) random.nextLong()) % this.totalWeight);

        if (entry != null) {
            return entry.model.getQuads(state, face, random, modelData);
        }

        return Collections.emptyList();
    }

    private static <T extends WeightedBakedModel.WeightedModel> T getAt(List<T> pool, int totalWeight) {
        int i = 0;
        int len = pool.size();

        T weighted;

        do {
            if (i >= len) {
                return null;
            }

            weighted = pool.get(i++);
            totalWeight -= weighted.weight;
        } while (totalWeight >= 0);

        return weighted;
    }

    @Override
    public @Nullable BakedModel embeddium$getInnerModel(Random rand) {
        WeightedBakedModel.WeightedModel entry = getAt(this.list, Math.abs((int) rand.nextLong()) % this.totalWeight);

        if (entry != null && entry.model.getClass() == SimpleBakedModel.class) {
            return entry.model;
        }

        return null;
    }
}
