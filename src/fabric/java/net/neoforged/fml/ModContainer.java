package net.neoforged.fml;

import net.neoforged.fml.loading.moddiscovery.ModInfo;

public record ModContainer(net.fabricmc.loader.api.ModContainer fabricContainer, ModInfo info) {
    public ModContainer(net.fabricmc.loader.api.ModContainer container) {
        this(container, new ModInfo(container));
    }

    public ModInfo getModInfo() {
        return info;
    }

    public <T> void registerExtensionPoint(Class<T> clz, T extensionPoint) {

    }
}
