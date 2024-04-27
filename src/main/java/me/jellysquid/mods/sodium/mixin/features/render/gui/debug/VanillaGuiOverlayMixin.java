package me.jellysquid.mods.sodium.mixin.features.render.gui.debug;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(VanillaGuiOverlay.class)
public class VanillaGuiOverlayMixin {
    /**
     * @author embeddedt
     * @reason Fix F3 text not being drawn using batching
     */
    @WrapOperation(method = "lambda$static$18", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/gui/overlay/ForgeGui;renderHUDText(IILnet/minecraft/client/gui/GuiGraphics;)V", remap = false), remap = false)
    private static void embeddium$renderTextManaged(ForgeGui instance, int width, int height, GuiGraphics gui, Operation<Void> original) {
        gui.drawManaged(() -> {
            original.call(instance, width, height, gui);
        });
    }
}
