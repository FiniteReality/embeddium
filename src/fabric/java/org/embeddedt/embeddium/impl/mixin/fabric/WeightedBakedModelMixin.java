package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.WeightedBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;

@Mixin(value = WeightedBakedModel.class, priority = 100)
public abstract class WeightedBakedModelMixin implements IBakedModelExtension {
    @Shadow
    public abstract List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random);

    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState p_235039_, @Nullable Direction p_235040_, RandomSource p_235041_, ModelData data, RenderType renderType) {
        return getQuads(p_235039_, p_235040_, p_235041_);
    }

    @Override
    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource source, ModelData modelData) {
        return IBakedModelExtension.super.getRenderTypes(state, source, modelData);
    }
}
