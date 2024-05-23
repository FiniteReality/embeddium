package net.neoforged.fml;

import net.neoforged.fml.loading.moddiscovery.ModInfo;

public class ModContainer {
    public static final ModContainer INSTANCE = new ModContainer();

    public static final ModInfo INFO = new ModInfo("embeddium");

    public ModInfo getModInfo() {
        return INFO;
    }
}
