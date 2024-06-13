package net.neoforged.fml.loading;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Supplier;

public class FMLPaths {
    public static final Supplier<Path> GAMEDIR = () -> Paths.get("").toAbsolutePath();
    public static final Supplier<Path> CONFIGDIR = () -> GAMEDIR.get().resolve("config");
}
