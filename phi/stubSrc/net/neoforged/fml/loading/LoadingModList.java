package net.neoforged.fml.loading;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.List;

public class LoadingModList {
    public static final LoadingModList INSTANCE = new LoadingModList();

    public static LoadingModList get() {
        return INSTANCE;
    }

    public boolean hasErrors() {
        return false;
    }

    public List<ModInfo> getMods() {
        return List.of(ModContainer.INFO);
    }

    public ModFileInfo getModFileById(String id) {
        return id.equals("embeddium") ? ModFileInfo.EMBEDDIUM : null;
    }

    public List<ModFileInfo> getModFiles() {
        return List.of(ModFileInfo.EMBEDDIUM);
    }
}
