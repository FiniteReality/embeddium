package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.Holder;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;


@Mixin(ItemBlockRenderTypes.class)
public class MixinRenderLayers {
    // Note: The Sodium patch to replace the backing collection is removed, because Forge uses fastutil

    @Shadow @Final private static Map<Holder.Reference<Block>, ChunkRenderTypeSet> BLOCK_RENDER_TYPES;

    static {
        // This is a temporary fix to solve frogspawn blocks making the underlying water invisible if translucency sorting
        // is off.
        // This slightly affects the look of the block, but is better than the alternative for now.
        BLOCK_RENDER_TYPES.put(ForgeRegistries.BLOCKS.getDelegateOrThrow(Blocks.FROGSPAWN), ChunkRenderTypeSet.of(RenderType.cutoutMipped()));
    }

    @Unique
    private static boolean embeddium$leavesFancy;

    @Redirect(
            method = { "getChunkRenderType", "getMovingBlockRenderType", "getRenderLayers" },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;renderCutout:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return embeddium$leavesFancy;
    }

    @Inject(method = "setFancy", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        embeddium$leavesFancy = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphicsOrBetter ? GraphicsStatus.FANCY : GraphicsStatus.FAST);
    }
}
