package net.neoforged.fml.loading;

import net.neoforged.api.distmarker.Dist;

public class FMLLoader {
    public static Runnable progressWindowTick;

    public static Dist getDist() {
        return Dist.CLIENT;
    }

    public static boolean isProduction() {
        return false;
    }

    public static LoadingModList getLoadingModList() {
        return LoadingModList.INSTANCE;
    }

    public record VersionInfo(String mcVersion, String neoForgeVersion) {}

    public static VersionInfo versionInfo() {
        return new VersionInfo("0.0.0" ,"0.0.0");
    }
}
