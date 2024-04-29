package org.embeddedt.embeddium.client.gui.options;

import net.fabricmc.loader.api.FabricLoader;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Stream;

public class OptionIdGenerator {
    private static final List<String> BLACKLISTED_PREFIXES = List.of("me.jellysquid.mods.sodium", "org.embeddedt.embeddium", "net.minecraft", "net.neoforged");

    private static boolean isAllowedClass(String name) {
        for(String prefix : BLACKLISTED_PREFIXES) {
            if(name.startsWith(prefix)) {
                return false;
            }
        }
        return true;
    }

    private static Stream<Class<?>> streamFromName(String name) {
        try {
            return Stream.of(Class.forName(name));
        } catch(Throwable e) {
            return Stream.empty();
        }
    }

    private static final Map<Path, String> MOD_ID_FROM_URL_CACHE = new HashMap<>();

    static {
        for(var info : FabricLoader.getInstance().getAllMods()) {
            for(Path rootPath : info.getRootPaths()) {
                MOD_ID_FROM_URL_CACHE.put(rootPath, info.getMetadata().getId());
            }
        }
    }

    public static <T> OptionIdentifier<T> generateId(String path) {
        var modId = StackWalker.getInstance().walk(frameStream -> {
            return frameStream
                    .map(StackWalker.StackFrame::getClassName)
                    .filter(OptionIdGenerator::isAllowedClass)
                    .flatMap(OptionIdGenerator::streamFromName)
                    .map(clz -> {
                        try {
                            var source = clz.getProtectionDomain().getCodeSource();
                            if(source != null) {
                                URL url = source.getLocation();
                                if(url != null) {
                                    return MOD_ID_FROM_URL_CACHE.get(Paths.get(url.toURI()));
                                }
                            }
                        } catch(URISyntaxException ignored) {}
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .findFirst();
        });
        return modId.filter(id -> !id.equals("minecraft")).map(s -> (OptionIdentifier<T>) OptionIdentifier.create(s, path)).orElse(null);
    }
}
