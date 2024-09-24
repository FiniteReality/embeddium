package net.neoforged.neoforge.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.ItemOverrides;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.util.TriState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class BakedModelWrapper<T extends BakedModel> {
    protected final T delegate;

    public BakedModelWrapper(T delegate) {
        this.delegate = delegate;
    }

    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction direction, RandomSource random) {
        return delegate.getQuads(state, direction, random);
    }

    public boolean isGui3d() {
        return delegate.isGui3d();
    }

    public List<BakedQuad> getQuads(@Nullable BlockState p_235039_, @Nullable Direction p_235040_, RandomSource p_235041_, ModelData data, RenderType renderType) {
        return delegate.getQuads(p_235039_, p_235040_, p_235041_, data, renderType);
    }

    public TriState useAmbientOcclusion(BlockState state, ModelData data, RenderType type) {
        return delegate.useAmbientOcclusion(state, data, type);
    }

    public ModelData getModelData(BlockAndTintGetter blockAndTintGetter, BlockPos blockPos, BlockState blockState, ModelData modelData) {
        return delegate.getModelData(blockAndTintGetter, blockPos, blockState, modelData);
    }

    public ChunkRenderTypeSet getRenderTypes(BlockState state, RandomSource source, ModelData modelData) {
        return delegate.getRenderTypes(state, source, modelData);
    }

    public TextureAtlasSprite getParticleIcon() {
        return delegate.getParticleIcon();
    }

    public boolean useAmbientOcclusion() {
        return delegate.useAmbientOcclusion();
    }

    public boolean isCustomRenderer() {
        return delegate.isCustomRenderer();
    }

    public ItemOverrides getOverrides() {
        return delegate.getOverrides();
    }

    public boolean usesBlockLight() {
        return delegate.usesBlockLight();
    }

    public ItemTransforms getTransforms() {
        return delegate.getTransforms();
    }
}
