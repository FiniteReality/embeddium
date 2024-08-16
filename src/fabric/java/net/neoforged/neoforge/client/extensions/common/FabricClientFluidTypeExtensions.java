package net.neoforged.neoforge.client.extensions.common;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandler;
import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class FabricClientFluidTypeExtensions implements IClientFluidTypeExtensions {
    private static final Map<Class<? extends FluidRenderHandler>, Boolean> handlersUsingCustomRenderer = new ConcurrentHashMap<>();

    private FluidRenderHandler getHandler(FluidState state) {
        var handler = FluidRenderHandlerRegistry.INSTANCE.get(state.getType());

        // Match the vanilla FluidRenderer's behavior if the handler is null
        if (handler == null) {
            boolean isLava = state.is(FluidTags.LAVA);
            handler = FluidRenderHandlerRegistry.INSTANCE.get(isLava ? Fluids.LAVA : Fluids.WATER);
        }

        return handler;
    }

    @Override
    public int getTintColor(FluidState state, BlockAndTintGetter view, BlockPos pos) {
        return (getHandler(state).getFluidColor(view, pos, state) | 0xFF000000);
    }

    @Override
    public boolean renderFluid(FluidState fluidState, BlockAndTintGetter world, BlockPos blockPos, VertexConsumer vertexBuilder, BlockState blockState) {
        var handler = getHandler(fluidState);

        // TODO move to a separate class
        boolean overridesRenderFluid = handlersUsingCustomRenderer.computeIfAbsent(handler.getClass(), (Function<? super Class<? extends FluidRenderHandler>, Boolean>)handlerClass -> {
            try {
                var method = handlerClass.getMethod("renderFluid", BlockPos.class, BlockAndTintGetter.class, VertexConsumer.class, BlockState.class, FluidState.class);
                return method.getDeclaringClass() != FluidRenderHandler.class;
            } catch (NoSuchMethodException e) {
                throw new RuntimeException("Unable to find renderFluid method. Possibly a mismatched Fabric API version?", e);
            }
        });

        if (!overridesRenderFluid) {
            // Use our renderer for higher performance
            return false;
        }

        handler.renderFluid(blockPos, world, vertexBuilder, blockState, fluidState);

        return true;
    }

    @Nullable
    private ResourceLocation getTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos, int idx) {
        var sprites = getHandler(fluidState).getFluidSprites(world, pos, fluidState);
        if(idx < sprites.length) {
            return sprites[idx].contents().name();
        } else {
            return null;
        }
    }

    @Override
    public ResourceLocation getStillTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos) {
        return getTexture(fluidState, world, pos, 0);
    }

    @Override
    public ResourceLocation getFlowingTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos) {
        return getTexture(fluidState, world, pos, 1);
    }

    @Override
    public ResourceLocation getOverlayTexture(FluidState fluidState, BlockAndTintGetter world, BlockPos pos) {
        return getTexture(fluidState, world, pos, 2);
    }
}
