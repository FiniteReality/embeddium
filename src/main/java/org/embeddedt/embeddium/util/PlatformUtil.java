package org.embeddedt.embeddium.util;

import net.minecraftforge.fml.loading.FMLLoader;

public class PlatformUtil {
    public static boolean isLoadValid() {
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }

    public static boolean modPresent(String modid) {
        return FMLLoader.getLoadingModList().getModFileById(modid) != null;
    }

    public static boolean isDevelopmentEnvironment() {
        return !FMLLoader.isProduction();
    }
}
