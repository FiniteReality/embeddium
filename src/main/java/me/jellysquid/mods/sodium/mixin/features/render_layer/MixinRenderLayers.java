package me.jellysquid.mods.sodium.mixin.features.render_layer;

import java.util.Map;
import java.util.function.Predicate;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LeavesBlock;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraftforge.registries.IRegistryDelegate;

@Mixin(RenderLayers.class)
public class MixinRenderLayers {
	
	@Shadow private static boolean fancyGraphicsOrBetter;
	@Shadow @Final private static Map<IRegistryDelegate<Block>, Predicate<RenderLayer>> blockRenderChecks;
    @Shadow @Final private static Map<IRegistryDelegate<Fluid>, Predicate<RenderLayer>> fluidRenderChecks;
	
    @Inject(remap = false, at = @At("HEAD"), method = "canRenderInLayer(Lnet/minecraft/block/BlockState;Lnet/minecraft/client/render/RenderLayer;)Z", cancellable = true)
    private static void render(BlockState state, RenderLayer type, CallbackInfoReturnable<Boolean> cir)
    {
        Block block = state.getBlock();
        if (block instanceof LeavesBlock)
        {
            cir.setReturnValue(fancyGraphicsOrBetter ? type == RenderLayer.getCutoutMipped() : type == RenderLayer.getSolid());
        }
        else
        {
            Predicate<RenderLayer> rendertype;
            rendertype = blockRenderChecks.get(block.delegate);
            cir.setReturnValue(rendertype != null ? rendertype.test(type) : type == RenderLayer.getSolid());
        }
    }

    @Inject(remap = false, at = @At("HEAD"), method = "canRenderInLayer(Lnet/minecraft/fluid/FluidState;Lnet/minecraft/client/render/RenderLayer;)Z", cancellable = true)
    private static void render(FluidState fluid, RenderLayer type, CallbackInfoReturnable<Boolean> cir)
    {
        Predicate<RenderLayer> rendertype;
        rendertype = fluidRenderChecks.get(fluid.getFluid().delegate);
        cir.setReturnValue(rendertype != null ? rendertype.test(type) : type == RenderLayer.getSolid());
    }
	
}