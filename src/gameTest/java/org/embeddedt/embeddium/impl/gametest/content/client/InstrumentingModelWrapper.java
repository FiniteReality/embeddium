package org.embeddedt.embeddium.impl.gametest.content.client;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.BakedModelWrapper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class InstrumentingModelWrapper<T extends BakedModel> extends BakedModelWrapper<T> {
    private volatile boolean hasBeenCalled;

    public InstrumentingModelWrapper(T originalModel) {
        super(originalModel);
    }

    public void resetCalledFlag() {
        this.hasBeenCalled = false;
    }

    public boolean hasBeenCalled() {
        return this.hasBeenCalled;
    }

    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull RandomSource rand, @NotNull ModelData extraData, @Nullable RenderType renderType) {
        this.hasBeenCalled = true;
        return super.getQuads(state, side, rand, extraData, renderType);
    }
}
