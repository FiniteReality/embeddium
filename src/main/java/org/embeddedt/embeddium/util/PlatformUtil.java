package org.embeddedt.embeddium.util;

import net.fabricmc.loader.api.FabricLoader;

public class PlatformUtil {
    public static boolean isLoadValid() {
        return true;
    }

    public static boolean modPresent(String modid) {
        return FabricLoader.getInstance().isModLoaded(modid);
    }

    public static boolean isDevelopmentEnvironment() {
        return FabricLoader.getInstance().isDevelopmentEnvironment();
    }
}
