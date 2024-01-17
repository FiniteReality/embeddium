package me.jellysquid.mods.sodium.mixin.features.render_layer.leaves;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.IRegistryDelegate;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(ItemBlockRenderTypes.class)
public class MixinRenderLayers {
    @Mutable
    @Shadow
    @Final
    private static Map<IRegistryDelegate<Block>, Predicate<RenderType>> blockRenderChecks;

    @Mutable
    @Shadow
    @Final
    private static Map<IRegistryDelegate<Block>, Predicate<RenderType>> fluidRenderChecks;

    static {
        // Replace the backing collection types with something a bit faster, since this is a hot spot in chunk rendering.
        blockRenderChecks = new Object2ObjectOpenHashMap<>(blockRenderChecks);
        fluidRenderChecks = new Object2ObjectOpenHashMap<>(fluidRenderChecks);
    }

    @Unique
    private static boolean embeddium$leavesFancy;

    @Redirect(
            method = { "getChunkRenderType", "getMovingBlockRenderType", "canRenderInLayer(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/client/renderer/RenderType;)Z" },
            at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ItemBlockRenderTypes;renderCutout:Z"))
    private static boolean redirectLeavesShouldBeFancy() {
        return embeddium$leavesFancy;
    }

    @Inject(method = "setFancy", at = @At("RETURN"))
    private static void onSetFancyGraphicsOrBetter(boolean fancyGraphicsOrBetter, CallbackInfo ci) {
        embeddium$leavesFancy = SodiumClientMod.options().quality.leavesQuality.isFancy(fancyGraphicsOrBetter ? GraphicsStatus.FANCY : GraphicsStatus.FAST);
    }
}
