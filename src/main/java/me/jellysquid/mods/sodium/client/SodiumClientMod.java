package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.compat.ccl.CCLCompat;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod(SodiumClientMod.MODID)
public class SodiumClientMod {

    public static final String MODID = "embeddium";
    public static final String MODNAME = "Embeddium";

    private static SodiumGameOptions CONFIG = loadConfig();
    private static Logger LOGGER = LoggerFactory.getLogger(MODNAME);

    private static String MOD_VERSION;

    public static boolean cclLoaded = false;

    public SodiumClientMod() {
        SodiumPreLaunch.onPreLaunch();

        MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "embeddium", (a, b) -> true));

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        LOGGER = LoggerFactory.getLogger("Sodium");
        CONFIG = loadConfig();

        cclLoaded = ModList.get().isLoaded("codechickenlib");

        if(cclLoaded) {
            CCLCompat.init();
        }
    }

    public static SodiumGameOptions options() {
        if (CONFIG == null) {
            throw new IllegalStateException("Config not yet available");
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
}