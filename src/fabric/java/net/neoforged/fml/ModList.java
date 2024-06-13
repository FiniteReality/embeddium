package net.neoforged.fml;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;

import java.util.List;
import java.util.Optional;

public class ModList {
    public static final ModList INSTANCE = new ModList();

    public static ModList get() {
        return INSTANCE;
    }

    public static boolean isLoaded(String id) {
        return LoadingModList.get().getModFileById(id) != null;
    }

    public Optional<ModContainer> getModContainerById(String modId) {
        return LoadingModList.get().getModContainer(modId);
    }

    public List<ModFileInfo> getModFiles() {
        return LoadingModList.get().getModFiles();
    }
}
