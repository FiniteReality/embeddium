package net.neoforged.fml.loading.moddiscovery;

import net.fabricmc.loader.api.ModContainer;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public record ModFile(ModContainer container) {
    public Path findResource(String... components) {
        String filePath = String.join(File.separator, components);
        var path = container.findPath(filePath);
        return path.isPresent() ? path.get() : Paths.get("$SNOWMAN");
    }
}
