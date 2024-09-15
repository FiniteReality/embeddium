package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.extensions.IForgeBakedModel;
import net.minecraftforge.client.model.data.IModelData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.List;
import java.util.Random;

@Mixin(value = SimpleBakedModel.class, priority = 700)
public abstract class SimpleBakedModelMixin implements IForgeBakedModel {
    @Shadow
    public abstract List<BakedQuad> getQuads(@Nullable BlockState pState, @Nullable Direction pDirection, Random pRandom);

    /**
     * @author embeddedt
     * @reason avoid interface dispatch on getQuads() from our block renderer
     */
    @Intrinsic
    @Override
    public @NotNull List<BakedQuad> getQuads(@Nullable BlockState state, @Nullable Direction side, @NotNull Random rand, @NotNull IModelData data) {
        return this.getQuads(state, side, rand);
    }
}
