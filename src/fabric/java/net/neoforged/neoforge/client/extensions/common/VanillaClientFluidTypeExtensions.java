package net.neoforged.neoforge.client.extensions.common;

import com.google.common.base.Suppliers;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;

import java.util.function.Supplier;

public class VanillaClientFluidTypeExtensions implements IClientFluidTypeExtensions {
    @Override
    public int getTintColor(FluidState state, BlockAndTintGetter view, BlockPos pos) {
        return (state.is(FluidTags.LAVA) ? 16777215 : BiomeColors.getAverageWaterColor(view, pos)) | 0xFF000000;
    }

    @Override
    public boolean renderFluid(FluidState fluidState, BlockAndTintGetter world, BlockPos blockPos, VertexConsumer vertexBuilder, BlockState blockState) {
        return false;
    }

    private static final Supplier<ResourceLocation> WATER_STILL = Suppliers.memoize(() -> Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.WATER.defaultBlockState()).getParticleIcon().contents().name());
    private static final Supplier<ResourceLocation> LAVA_STILL = Suppliers.memoize(() -> Minecraft.getInstance().getModelManager().getBlockModelShaper().getBlockModel(Blocks.LAVA.defaultBlockState()).getParticleIcon().contents().name());

    @Override
    public ResourceLocation getStillTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos) {
        return fluidState.is(FluidTags.LAVA) ? LAVA_STILL.get() : WATER_STILL.get();
    }

    @Override
    public ResourceLocation getFlowingTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos) {
        return fluidState.is(FluidTags.LAVA) ? ModelBakery.LAVA_FLOW.texture() : ModelBakery.WATER_FLOW.texture();
    }

    @Override
    public ResourceLocation getOverlayTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos) {
        return fluidState.is(FluidTags.LAVA) ? null : ModelBakery.WATER_OVERLAY.texture();
    }
}
