package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.data.fingerprint.FingerprintMeasure;
import me.jellysquid.mods.sodium.client.data.fingerprint.HashedFingerprint;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.network.NetworkConstants;

import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.render.ShaderModBridge;
import org.embeddedt.embeddium.taint.incompats.IncompatibleModManager;
import org.embeddedt.embeddium.taint.scanning.TaintDetector;
import org.embeddedt.embeddium.util.sodium.FlawlessFrames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Mod(SodiumClientMod.MODID)
public class SodiumClientMod {
    public static final String MODID = EmbeddiumConstants.MODID;
    public static final String MODNAME = EmbeddiumConstants.MODNAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    private static SodiumGameOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    public SodiumClientMod() {
        MOD_VERSION = ModList.get().getModContainerById(MODID).get().getModInfo().getVersion().toString();
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> NetworkConstants.IGNORESERVERONLY, (a, b) -> true));

        if (!FMLLoader.getDist().isClient()) {
            return;
        }

        TaintDetector.init();

        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::onClientSetup);

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        IncompatibleModManager.checkMods(event);
        FlawlessFrames.onClientInitialization();
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

    private static void updateFingerprint() {
        var current = FingerprintMeasure.create();

        if (current == null) {
            return;
        }

        HashedFingerprint saved = null;

        try {
            saved = HashedFingerprint.loadFromDisk();
        } catch (Throwable t) {
            LOGGER.error("Failed to load existing fingerprint",  t);
        }

        if (saved == null || !current.looselyMatches(saved)) {
            HashedFingerprint.writeToDisk(current.hashed());

            CONFIG.notifications.hasSeenDonationPrompt = false;
            CONFIG.notifications.hasClearedDonationButton = false;

            try {
                CONFIG.writeChanges();
            } catch (IOException e) {
                LOGGER.error("Failed to update config file", e);
            }
        }
    }

    public static boolean canUseVanillaVertices() {
        return !SodiumClientMod.options().performance.useCompactVertexFormat && !ShaderModBridge.areShadersEnabled();
    }

    public static boolean canApplyTranslucencySorting() {
        return SodiumClientMod.options().performance.useTranslucentFaceSorting && !ShaderModBridge.isNvidiumEnabled();
    }
}