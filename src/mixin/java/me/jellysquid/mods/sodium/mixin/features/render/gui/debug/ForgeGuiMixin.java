package me.jellysquid.mods.sodium.mixin.features.render.gui.debug;

import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import net.minecraftforge.fml.util.ObfuscationReflectionHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;

@Mixin(ForgeGui.class)
public abstract class ForgeGuiMixin extends Gui {
    private DebugScreenOverlay embeddium$debugOverlay;

    public ForgeGuiMixin(Minecraft pMinecraft, ItemRenderer pItemRenderer) {
        super(pMinecraft, pItemRenderer);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void accessDebugOverlay(Minecraft mc, CallbackInfo ci) {
        embeddium$debugOverlay = ObfuscationReflectionHelper.getPrivateValue(ForgeGui.class, (ForgeGui)(Object)this, "debugOverlay");
    }

    /**
     * @author embeddedt
     * @reason Use the vanilla code to render lines, which fills all backgrounds first before drawing text, so that
     * batching works correctly. Also, ensure the lines are rendered in a managed context.
     */
    @Inject(method = "renderHUDText", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/eventbus/api/IEventBus;post(Lnet/minecraftforge/eventbus/api/Event;)Z", shift = At.Shift.AFTER, remap = false), remap = false)
    private void renderLinesVanilla(int width, int height, GuiGraphics guiGraphics, CallbackInfo ci, @Local(ordinal = 0) ArrayList<String> listL, @Local(ordinal = 1) ArrayList<String> listR) {
        DebugScreenOverlayAccessor accessor = (DebugScreenOverlayAccessor)embeddium$debugOverlay;
        guiGraphics.drawManaged(() -> {
            accessor.invokeRenderLines(guiGraphics, listL, true);
            accessor.invokeRenderLines(guiGraphics, listR, false);
        });
        // Prevent Forge from rendering any lines
        listL.clear();
        listR.clear();
    }
}
