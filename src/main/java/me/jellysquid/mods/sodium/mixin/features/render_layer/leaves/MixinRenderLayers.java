package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.option.GraphicsMode;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.util.registry.RegistryEntry;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;


@Mixin(RenderLayers.class)
public class MixinRenderLayers {
    // Note: The Sodium patch to replace the backing collection is removed, because Forge uses fastutil

    @Shadow @Final private static Map<RegistryEntry.Reference<Block>, ChunkRenderTypeSet> BLOCK_RENDER_TYPES;

    static {
        // This is a temporary fix to solve frogspawn blocks making the underlying water invisible if translucency sorting
        // is off.
        // This slightly affects the look of the block, but is better than the alternative for now.
        BLOCK_RENDER_TYPES.put(ForgeRegistries.BLOCKS.getDelegateOrThrow(Blocks.FROGSPAWN), ChunkRenderTypeSet.of(RenderLayer.getCutoutMipped()));
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
