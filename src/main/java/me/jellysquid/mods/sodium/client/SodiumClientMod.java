package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.data.fingerprint.FingerprintMeasure;
import me.jellysquid.mods.sodium.client.data.fingerprint.HashedFingerprint;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.packs.PackType;
import org.embeddedt.embeddium.render.frapi.SpriteFinderCache;
import org.embeddedt.embeddium.render.ShaderModBridge;
import org.embeddedt.embeddium.taint.scanning.TaintDetector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class SodiumClientMod implements ClientModInitializer {

    public static final String MODID = "embeddium";
    public static final String MODNAME = "Embeddium";

    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    private static SodiumGameOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    @Override
    public void onInitializeClient() {
        MOD_VERSION = FabricLoader.getInstance().getModContainer(MODID).get().getMetadata().getVersion().toString();

        TaintDetector.init();

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }

        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(SpriteFinderCache.ReloadListener.INSTANCE);
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