package net.neoforged.fml.loading.moddiscovery;

import net.neoforged.fml.ModContainer;

import java.util.List;

public record ModFileInfo(ModFile file) {
    public static final ModFileInfo EMBEDDIUM = new ModFileInfo(new ModFile());

    public ModFile getFile() {
        return this.file;
    }

    public String versionString() {
        return "0.0.0";
    }

    public List<ModInfo> getMods() {
        return List.of(ModContainer.INFO);
    }
}
