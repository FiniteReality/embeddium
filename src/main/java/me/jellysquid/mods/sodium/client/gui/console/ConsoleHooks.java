package me.jellysquid.mods.sodium.client.gui.console;

import com.mojang.blaze3d.vertex.PoseStack;

public class ConsoleHooks {
    public static void render(PoseStack drawContext, double currentTime) {
        ConsoleRenderer.INSTANCE.update(Console.INSTANCE, currentTime);
        ConsoleRenderer.INSTANCE.draw(drawContext);
    }
}
