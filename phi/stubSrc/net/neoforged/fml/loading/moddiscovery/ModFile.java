package net.neoforged.fml.loading.moddiscovery;

import java.io.File;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ModFile {
    public Path findResource(String... components) {
        String path = String.join(File.separator, components);
        URL url = ModFile.class.getClassLoader().getResource(path);
        if(url != null) {
            try {
                return Paths.get(url.toURI());
            } catch(Throwable e) {
                e.printStackTrace();
            }
        }
        return Paths.get("$SNOWMAN");
    }
}
