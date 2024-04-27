package me.jellysquid.mods.sodium.mixin.features.render.gui.debug;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.DebugScreenOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.List;

@Mixin(DebugScreenOverlay.class)
public interface DebugScreenOverlayAccessor {
    @Invoker("renderLines")
    void invokeRenderLines(GuiGraphics pGuiGraphics, List<String> pLines, boolean pLeftSide);
}
