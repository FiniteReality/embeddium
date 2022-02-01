package me.jellysquid.mods.sodium;

import me.jellysquid.mods.sodium.config.user.UserConfig;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;

@Mod(SodiumClientMod.MODID)
public class SodiumClientMod {
    private static UserConfig CONFIG;
    private static Logger LOGGER;

    private static String MOD_VERSION = "0.4.0d";
    
    public static final String MODID = "rubidium";
    
    public SodiumClientMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }
    
    public void setup(final FMLClientSetupEvent event) {
        LOGGER = LogManager.getLogger("Rubidium");
        CONFIG = loadConfig();
    }

    public static UserConfig options() {
        if (CONFIG == null) {
        	CONFIG = loadConfig();
        }

        return CONFIG;
    }

    public static Logger logger() {
        if (LOGGER == null) {
            throw new IllegalStateException("Logger not yet available");
        }

        return LOGGER;
    }

    private static UserConfig loadConfig() {
        try {
            return UserConfig.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new UserConfig();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = UserConfig.defaults();

        try {
            CONFIG.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        return MOD_VERSION;
    }

    public static boolean isDirectMemoryAccessEnabled() {
        return options().advanced.allowDirectMemoryAccess;
    }
}
