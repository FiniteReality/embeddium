package net.neoforged.neoforge.client.gui;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;

public interface IConfigScreenFactory {
    public Screen getScreen(Minecraft mc, Screen prevScreen);
}
