package me.jellysquid.mods.sodium.mixin.features.textures.mipmaps;

import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(RenderType.class)
public class RenderTypeMixin {
    /**
     * @author coderbot16 (in Iris), embeddedt
     * @reason Force cutout/cutout_mipped to use 0.1F alpha cutoff to match modern handling
     */
    @ModifyArg(method = "<clinit>", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/RenderType$CompositeState$CompositeStateBuilder;setAlphaState(Lnet/minecraft/client/renderer/RenderStateShard$AlphaStateShard;)Lnet/minecraft/client/renderer/RenderType$CompositeState$CompositeStateBuilder;"), index = 0)
    private static RenderStateShard.AlphaStateShard embeddium$tweakCutoutAlpha(RenderStateShard.AlphaStateShard arg) {
        return new RenderStateShard.AlphaStateShard(0.1F);
    }
}
