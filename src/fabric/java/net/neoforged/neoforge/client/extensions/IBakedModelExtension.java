package net.neoforged.neoforge.client.extensions;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IBakedModelExtension {
    default List<BakedQuad> getQuads(@Nullable BlockState p_235039_, @Nullable Direction p_235040_, RandomSource p_235041_, net.neoforged.neoforge.client.model.data.ModelData data, net.minecraft.client.renderer.RenderType renderType) {
       return ((BakedModel)this).getQuads(p_235039_, p_235040_, p_235041_);
    }

    default net.neoforged.neoforge.common.util.TriState useAmbientOcclusion(BlockState state, net.neoforged.neoforge.client.model.data.ModelData data, RenderType type) {
        return ((BakedModel)this).useAmbientOcclusion() ? net.neoforged.neoforge.common.util.TriState.DEFAULT : net.neoforged.neoforge.common.util.TriState.FALSE;
    }
    default net.neoforged.neoforge.client.model.data.ModelData getModelData(net.minecraft.world.level.BlockAndTintGetter blockAndTintGetter, net.minecraft.core.BlockPos blockPos, BlockState blockState, net.neoforged.neoforge.client.model.data.ModelData modelData) {
        return modelData;
    }

    default net.neoforged.neoforge.client.ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource source, net.neoforged.neoforge.client.model.data.ModelData modelData) {
        return ChunkRenderTypeSet.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }
}
