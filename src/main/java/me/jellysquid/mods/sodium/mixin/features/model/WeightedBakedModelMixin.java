package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import org.embeddedt.embeddium.model.UnwrappableBakedModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.*;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements UnwrappableBakedModel {
    @Shadow
    @Final
    private List<WeightedEntry.Wrapper<BakedModel>> list;

    @Shadow
    @Final
    private int totalWeight;

    /**
     * The running totals of the weights of the entries in {@link #list}. Neither the list nor the weights change after
     * the model has been constructed.
     */
    @Unique
    private int[] embeddium$cumulativeWeights;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void computeCumulativeWeights(CallbackInfo ci) {
        int[] cumulativeWeights = new int[this.list.size()];
        int sum = 0;

        for (int i = 0; i < cumulativeWeights.length; i++) {
            sum += this.list.get(i).getWeight().asInt();
            cumulativeWeights[i] = sum;
        }

        this.embeddium$cumulativeWeights = cumulativeWeights;
    }

    /**
     * @author JellySquid
     * @reason Avoid excessive object allocations
     */
    @Overwrite(remap = false)
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random, ModelData modelData, RenderType renderLayer) {
        WeightedEntry.Wrapper<BakedModel> quad = this.getAt(Math.abs((int) random.nextLong()) % this.totalWeight);

        if (quad != null) {
            return quad.getData()
                    .getQuads(state, face, random, modelData, renderLayer);
        }

        return Collections.emptyList();
    }

    /**
     * @author embeddedt
     * @reason Avoid excessive object allocations
     */
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        WeightedEntry.Wrapper<BakedModel> quad = this.getAt(Math.abs((int) rand.nextLong()) % this.totalWeight);

        if (quad != null) {
            return quad.getData().getRenderTypes(state, rand, data);
        }

        return ChunkRenderTypeSet.none();
    }

    /**
     * Finds the entry which the given weighted index selects. Vanilla walks the list, subtracting each entry's weight
     * from the index, and stops at the first entry for which the result is negative. Weights are never negative, so
     * that is the first entry whose running total is greater than the index, and the entries need not be read.
     */
    @Unique
    private WeightedEntry.Wrapper<BakedModel> getAt(int weightedIndex) {
        int[] cumulativeWeights = this.embeddium$cumulativeWeights;

        for (int i = 0; i < cumulativeWeights.length; i++) {
            if (cumulativeWeights[i] > weightedIndex) {
                return this.list.get(i);
            }
        }

        return null;
    }

    @Override
    public @Nullable BakedModel embeddium$getInnerModel(RandomSource rand) {
        WeightedEntry.Wrapper<BakedModel> quad = this.getAt(Math.abs((int) rand.nextLong()) % this.totalWeight);

        if (quad != null && quad.getData().getClass() == SimpleBakedModel.class) {
            return quad.getData();
        }

        return null;
    }
}
