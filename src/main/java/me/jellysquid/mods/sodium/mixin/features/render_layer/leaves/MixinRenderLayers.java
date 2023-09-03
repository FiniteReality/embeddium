package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.fluid.Fluid;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

@Mixin(RenderLayers.class)
public class MixinRenderLayers {
    @Mutable
    @Shadow
    @Final
    private static Map<Block, RenderLayer> BLOCKS;

    @Mutable
    @Shadow
    @Final
    private static Map<Fluid, RenderLayer> FLUIDS;

    static {
        // Replace the backing collection types with something a bit faster, since this is a hot spot in chunk rendering.
        BLOCKS = new Reference2ReferenceOpenHashMap<>(BLOCKS);
        
        // TODO: This is a temporary fix to solve frogspawn blocks making the underlying water invisible due to translucency sorting.
        // This slightly affects the look of the block, but is better than the alternative for now.
        BLOCKS.replace(Blocks.FROGSPAWN, RenderLayer.getCutoutMipped());
        
        FLUIDS = new Reference2ReferenceOpenHashMap<>(FLUIDS);
    }

    @Unique
    private static boolean embeddium$leavesFancy;

    @Redirect(
            method = { "getBlockLayer", "getMovingBlockLayer", "getRenderLayers" },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/render/RenderLayers;fancyGraphicsOrBetter:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return embeddium$leavesFancy;
    }

    @Inject(method = "setFancyGraphicsOrBetter", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        embeddium$leavesFancy = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphicsOrBetter ? GraphicsMode.FANCY : GraphicsMode.FAST);
    }
}
