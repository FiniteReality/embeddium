package org.embeddedt.embeddium.util;

import net.neoforged.fml.loading.FMLLoader;

public class PlatformUtil {
    public static boolean isLoadValid() {
        return FMLLoader.getLoadingModList().getErrors().isEmpty();
    }
}
