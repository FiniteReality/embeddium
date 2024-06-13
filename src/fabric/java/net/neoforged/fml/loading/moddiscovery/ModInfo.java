package net.neoforged.fml.loading.moddiscovery;

import net.fabricmc.loader.api.ModContainer;
import net.neoforged.neoforgespi.language.IModInfo;

import java.util.List;
import java.util.Optional;

public record ModInfo(ModContainer fabricContainer) {
    public String getModId() {
        return fabricContainer.getMetadata().getId();
    }

    public Optional<?> getConfigElement(String key) {
        return Optional.empty();
    }

    public String getVersion() {
        return fabricContainer.getMetadata().getVersion().getFriendlyString();
    }

    public Optional<String> getLogoFile() {
        return Optional.empty();
    }

    public String getDisplayName() {
        return fabricContainer.getMetadata().getName();
    }

    public List<IModInfo.ModVersion> getDependencies() {
        return List.of();
    }
}
