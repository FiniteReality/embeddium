package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import java.io.IOException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod(SodiumClientMod.MODID)
public class SodiumClientMod {
    private static SodiumGameOptions CONFIG;
    private static Logger LOGGER = LoggerFactory.getLogger("Rubidium");

    private static String MOD_VERSION;

    public static final String MODID = "rubidium";
    
    public static boolean flywheelLoaded = false;
    public static boolean oculusLoaded = false;
    
    public SodiumClientMod() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
        MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
    }
    
    public void setup(final FMLClientSetupEvent event) {
        CONFIG = loadConfig();
        flywheelLoaded = ModList.get().isLoaded("flywheel");
        oculusLoaded = ModList.get().isLoaded("oculus");
    }

    public static SodiumGameOptions options() {
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

    private static SodiumGameOptions loadConfig() {
        try {
            return SodiumGameOptions.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new SodiumGameOptions();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = SodiumGameOptions.defaults();

        try {
            CONFIG.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Failed to write config file", e);
        }
    }

    public static String getVersion() {
        if (MOD_VERSION == null) {
            throw new NullPointerException("Mod version hasn't been populated yet");
        }

        return MOD_VERSION;
    }

    public static boolean isDirectMemoryAccessEnabled() {
        return options().advanced.allowDirectMemoryAccess;
    }
}
