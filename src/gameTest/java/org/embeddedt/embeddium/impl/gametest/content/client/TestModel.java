package org.embeddedt.embeddium.impl.gametest.content.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.embeddedt.embeddium.impl.gametest.content.TestBlockEntity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

public class TestModel extends BakedModelWrapper<BakedModel> {
    public static final ModelProperty<Object> TEST_MODEL_PROPERTY = new ModelProperty<>();
    public static final Object TEST_MODEL_VALUE = new Object();

    public TestModel(BakedModel originalModel) {
        super(originalModel);
    }

    // Intentionally override the vanilla method, so that we trigger failure
    @Override
    public List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, RandomSource rand) {
        return getQuads(state, side, rand, ModelData.EMPTY, null);
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        if(!Objects.equals(extraData.get(TEST_MODEL_PROPERTY), TEST_MODEL_VALUE)) {
            throw new IllegalStateException("Missing model data value injected by BakedModel#getModelData");
        }
        if(!Objects.equals(extraData.get(TestBlockEntity.TEST_PROPERTY), TestBlockEntity.TEST_VALUE)) {
            throw new IllegalStateException("Missing model data value injected by BlockEntity#getModelData");
        }
        return super.getQuads(state, side, rand, extraData, renderType);
    }

    @Override
    public @NotNull ModelData getModelData(@NotNull BlockAndTintGetter level, @NotNull BlockPos pos, @NotNull BlockState state, @NotNull ModelData modelData) {
        if(modelData != TestBlockEntity.TEST_DATA) {
            throw new IllegalStateException("Model data provided to model is not from block's entity");
        }
        return modelData.derive().with(TEST_MODEL_PROPERTY, TEST_MODEL_VALUE).build();
    }
}
