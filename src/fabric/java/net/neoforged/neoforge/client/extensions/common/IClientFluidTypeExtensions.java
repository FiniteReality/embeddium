package net.neoforged.neoforge.client.extensions.common;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import org.jetbrains.annotations.Nullable;

public interface IClientFluidTypeExtensions {
    IClientFluidTypeExtensions INSTANCE = FabricLoader.getInstance().isModLoaded("fabric-rendering-fluids-v1") ? new FabricClientFluidTypeExtensions() : new VanillaClientFluidTypeExtensions();

    static IClientFluidTypeExtensions of(FluidState state) {
        return INSTANCE;
    }

    static IClientFluidTypeExtensions of(Fluid fluid) {
        return INSTANCE;
    }

    int getTintColor(FluidState state, BlockAndTintGetter view, BlockPos pos);

    boolean renderFluid(FluidState fluidState, BlockAndTintGetter world, BlockPos blockPos, VertexConsumer vertexBuilder, BlockState blockState);

    ResourceLocation getStillTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos);

    ResourceLocation getFlowingTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos);

    @Nullable
    ResourceLocation getOverlayTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos);
}
