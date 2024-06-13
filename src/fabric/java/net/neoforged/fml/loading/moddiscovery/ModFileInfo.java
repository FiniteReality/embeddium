package net.neoforged.fml.loading.moddiscovery;

import net.neoforged.fml.ModContainer;

import java.util.List;

public record ModFileInfo(ModContainer container, ModFile file) {
    public ModFileInfo(ModContainer container) {
        this(container, new ModFile(container.fabricContainer()));
    }

    public ModFile getFile() {
        return this.file;
    }

    public String versionString() {
        return container.getModInfo().getVersion();
    }

    public List<ModInfo> getMods() {
        return List.of(container.getModInfo());
    }
}
