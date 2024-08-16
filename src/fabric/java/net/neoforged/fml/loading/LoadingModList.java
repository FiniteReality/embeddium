package net.neoforged.fml.loading;

import net.fabricmc.loader.api.FabricLoader;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingIssue;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import net.neoforged.fml.loading.moddiscovery.ModInfo;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Function;
import java.util.stream.Collectors;

public class LoadingModList {
    public static final LoadingModList INSTANCE = new LoadingModList();

    private final Map<String, ModContainer> modContainers = FabricLoader.getInstance().getAllMods().stream().map(ModContainer::new).collect(Collectors.toMap(mc -> mc.getModInfo().getModId(), Function.identity()));
    private final List<ModInfo> modList = List.copyOf(modContainers.values().stream().map(ModContainer::getModInfo).toList());
    private final Map<String, ModFileInfo> modFileInfoMap;
    private final List<ModFileInfo> modFileList;

    private final List<ModLoadingIssue> loadingIssues = new CopyOnWriteArrayList<>();

    public LoadingModList() {
        this.modFileInfoMap = new HashMap<>();
        for(var info : this.modContainers.values()) {
            this.modFileInfoMap.put(info.getModInfo().getModId(), new ModFileInfo(info));
        }
        this.modFileList = List.copyOf(modFileInfoMap.values());
    }

    public static LoadingModList get() {
        return INSTANCE;
    }

    public boolean hasErrors() {
        return false;
    }

    public List<ModInfo> getMods() {
        return modList;
    }

    public ModFileInfo getModFileById(String id) {
        return modFileInfoMap.get(id);
    }

    public Optional<ModContainer> getModContainer(String id) {
        return Optional.ofNullable(modContainers.get(id));
    }

    public List<ModFileInfo> getModFiles() {
        return modFileList;
    }

    public List<ModLoadingIssue> getModLoadingIssues() {
        return loadingIssues;
    }
}
