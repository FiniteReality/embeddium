package org.embeddedt.embeddium.impl;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.impl.data.fingerprint.FingerprintMeasure;
import org.embeddedt.embeddium.impl.data.fingerprint.HashedFingerprint;
import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;
import org.embeddedt.embeddium.impl.gui.EmbeddiumVideoOptionsScreen;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.sodium.FlawlessFrames;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;

import java.io.IOException;

@Mod(value = Embeddium.MODID, dist = Dist.CLIENT)
public class Embeddium {
    public static final String MODID = EmbeddiumConstants.MODID;
    public static final String MODNAME = EmbeddiumConstants.MODNAME;

    private static final Logger LOGGER = LoggerFactory.getLogger(MODNAME);
    private static EmbeddiumOptions CONFIG = loadConfig();

    private static String MOD_VERSION;

    public Embeddium(IEventBus modEventBus) {
        var modContainer = ModList.get().getModContainerById(MODID).orElseThrow();
        MOD_VERSION = modContainer.getModInfo().getVersion().toString();
        modContainer.registerExtensionPoint(IConfigScreenFactory.class, (mc, screen) -> new EmbeddiumVideoOptionsScreen(screen, EmbeddiumVideoOptionsScreen.makePages()));

        if("true".equals(System.getProperty("embeddium.enableGameTest"))) {
            try {
                modEventBus.register(Class.forName("org.embeddedt.embeddium.impl.gametest.content.TestRegistry"));
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
        }

        try {
            updateFingerprint();
        } catch (Throwable t) {
            LOGGER.error("Failed to update fingerprint", t);
        }

        modEventBus.addListener(this::onClientSetup);
    }

    public void onClientSetup(final FMLClientSetupEvent event) {
        FlawlessFrames.onClientInitialization();
    }

    public static EmbeddiumOptions options() {
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

    private static EmbeddiumOptions loadConfig() {
        try {
            return EmbeddiumOptions.load();
        } catch (Exception e) {
            LOGGER.error("Failed to load configuration file", e);
            LOGGER.error("Using default configuration file in read-only mode");

            var config = new EmbeddiumOptions();
            config.setReadOnly();

            return config;
        }
    }

    public static void restoreDefaultOptions() {
        CONFIG = EmbeddiumOptions.defaults();

        try {
            EmbeddiumOptions.writeToDisk(CONFIG);
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
                EmbeddiumOptions.writeToDisk(CONFIG);
            } catch (IOException e) {
                LOGGER.error("Failed to update config file", e);
            }
        }
    }

    public static boolean canUseVanillaVertices() {
        return !Embeddium.options().performance.useCompactVertexFormat && !ShaderModBridge.areShadersEnabled();
    }

    public static boolean canApplyTranslucencySorting() {
        return Embeddium.options().performance.useTranslucentFaceSorting && !ShaderModBridge.isNvidiumEnabled();
    }
}
