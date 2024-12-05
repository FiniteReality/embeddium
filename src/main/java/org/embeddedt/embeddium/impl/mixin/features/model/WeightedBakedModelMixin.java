package org.embeddedt.embeddium.impl.mixin.features.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.SimpleWeightedRandomList;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedEntry.Wrapper;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.embeddedt.embeddium.impl.model.UnwrappableBakedModel;
import org.embeddedt.embeddium.impl.util.collections.WeightedRandomListExtended;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;

import java.util.*;

@Mixin(WeightedBakedModel.class)
public class WeightedBakedModelMixin implements UnwrappableBakedModel {
    @Shadow
    @Final
    private SimpleWeightedRandomList<BakedModel> list;

    private WeightedEntry.Wrapper<BakedModel> embeddium$readWeightedList(RandomSource random) {
        return ((WeightedRandomListExtended<Wrapper<BakedModel>>)this.list).embeddium$getRandomItem(random);
    }

    /**
     * @author JellySquid
     * @reason Avoid excessive object allocations
     */
    @Overwrite
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction face, RandomSource random, ModelData modelData, RenderType renderLayer) {
        var quad = embeddium$readWeightedList(random);

        if (quad != null) {
            return quad.data().getQuads(state, face, random, modelData, renderLayer);
        }

        return Collections.emptyList();
    }

    /**
     * @author embeddedt
     * @reason Avoid excessive object allocations
     */
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource rand, @NotNull ModelData data) {
        var quad = embeddium$readWeightedList(rand);

        if (quad != null) {
            return quad.data().getRenderTypes(state, rand, data);
        }

        return ChunkRenderTypeSet.none();
    }

    @Override
    public @Nullable BakedModel embeddium$getInnerModel(RandomSource rand) {
        var quad = embeddium$readWeightedList(rand);

        if (quad != null && quad.data().getClass() == SimpleBakedModel.class) {
            return quad.data();
        }

        return null;
    }
}
