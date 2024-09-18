package org.embeddedt.embeddium.util;

import net.fabricmc.loader.api.FabricLoader;

public class PlatformUtil {
    public static boolean isLoadValid() {
        return true;
    }

    public static boolean modPresent(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    public static String getModName(String modId) {
        return FabricLoader.getInstance().getModContainer(modId).map(container -> container.getMetadata().getName()).orElse(modId);
    }

    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
