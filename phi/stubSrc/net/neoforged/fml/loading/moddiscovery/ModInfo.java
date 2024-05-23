package net.neoforged.fml.loading.moddiscovery;

import net.neoforged.neoforgespi.language.IModInfo;

import java.util.List;
import java.util.Optional;

public record ModInfo(String modId) {
    public String getModId() {
        return modId;
    }

    public Optional<?> getConfigElement(String key) {
        return Optional.empty();
    }

    public String getVersion() {
        return "0.0.0";
    }

    public Optional<String> getLogoFile() {
        return Optional.empty();
    }

    public String getDisplayName() {
        return "Embeddium";
    }

    public List<IModInfo.ModVersion> getDependencies() {
        return List.of();
    }
}
