package me.jellysquid.mods.sodium.client.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonSyntaxException;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.TextProvider;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.config.ConfigMigrator;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static me.jellysquid.mods.sodium.client.SodiumClientMod.MODID;

public class SodiumGameOptions {
    private static final String DEFAULT_FILE_NAME = MODID + "-options.json";

    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();
    public final PerformanceSettings performance = new PerformanceSettings();
    public final NotificationSettings notifications = new NotificationSettings();

    private boolean readOnly;

    private Path configPath;

    public static SodiumGameOptions defaults() {
        var options = new SodiumGameOptions();
        options.configPath = getConfigPath(DEFAULT_FILE_NAME);

        return options;
    }

    public static class PerformanceSettings {
        public int chunkBuilderThreads = 0;
        @SerializedName("always_defer_chunk_updates_v2") // this will reset the option in older configs
        public boolean alwaysDeferChunkUpdates = true;

        public boolean animateOnlyVisibleTextures = true;
        public boolean useEntityCulling = true;
        public boolean useFogOcclusion = true;
        public boolean useBlockFaceCulling = true;
        public boolean useCompactVertexFormat = true;
        @SerializedName("use_translucent_face_sorting_v2")
        public boolean useTranslucentFaceSorting = true;
        public boolean useRenderPassOptimization = true;
        public boolean useNoErrorGLContext = true;
    }

    public static class AdvancedSettings {
        public boolean enableMemoryTracing = false;
        public boolean useAdvancedStagingBuffers = true;
        public boolean disableIncompatibleModWarnings = false;

        public int cpuRenderAheadLimit = 3;
    }

    public static class QualitySettings {
        public GraphicsQuality weatherQuality = GraphicsQuality.DEFAULT;
        public GraphicsQuality leavesQuality = GraphicsQuality.DEFAULT;

        public boolean enableVignette = true;

        public boolean useQuadNormalsForShading = false;
    }

    public static class NotificationSettings {
        public boolean forceDisableDonationPrompts = false;

        public boolean hasClearedDonationButton = false;
        public boolean hasSeenDonationPrompt = false;
    }

    public enum GraphicsQuality implements TextProvider {
        DEFAULT("options.gamma.default"),
        FANCY("options.clouds.fancy"),
        FAST("options.clouds.fast");

        private final Component name;

        GraphicsQuality(String name) {
            this.name = Component.translatable(name);
        }

        @Override
        public Component getLocalizedName() {
            return this.name;
        }

        public boolean isFancy(GraphicsStatus graphicsMode) {
            return (this == FANCY) || (this == DEFAULT && (graphicsMode == GraphicsStatus.FANCY || graphicsMode == GraphicsStatus.FABULOUS));
        }
    }

    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .setPrettyPrinting()
            .excludeFieldsWithModifiers(Modifier.PRIVATE)
            .create();

    public static SodiumGameOptions load() {
        return load(DEFAULT_FILE_NAME);
    }

    public static SodiumGameOptions load(String name) {
        Path path = getConfigPath(name);
        SodiumGameOptions config;
        boolean resaveConfig = true;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, SodiumGameOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            } catch (JsonSyntaxException e) {
                SodiumClientMod.logger().error("Could not parse config, will fallback to default settings", e);
                config = new SodiumGameOptions();
                resaveConfig = false;
            }
        } else {
            config = new SodiumGameOptions();
        }

        config.configPath = path;

        // TODO Embeddium: Remove the field completely in 0.4
        config.notifications.forceDisableDonationPrompts = false;

        try {
            if(resaveConfig)
                config.writeChanges();
        } catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }

        return config;
    }

    private static Path getConfigPath(String name) {
        return ConfigMigrator.handleConfigMigration(name);
    }

    @Deprecated
    public void writeChanges() throws IOException {
        writeToDisk(this);
    }

    public static void writeToDisk(SodiumGameOptions config) throws IOException {
        if (config.isReadOnly()) {
            throw new IllegalStateException("Config file is read-only");
        }

        Path dir = config.configPath.getParent();

        if (!Files.exists(dir)) {
            Files.createDirectories(dir);
        } else if (!Files.isDirectory(dir)) {
            throw new IOException("Not a directory: " + dir);
        }

        // Use a temporary location next to the config's final destination
        Path tempPath = config.configPath.resolveSibling(config.configPath.getFileName() + ".tmp");

        // Write the file to our temporary location
        Files.writeString(tempPath, GSON.toJson(config));

        // Atomically replace the old config file (if it exists) with the temporary file
        Files.move(tempPath, config.configPath, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    }

    public boolean isReadOnly() {
        return this.readOnly;
    }

    public void setReadOnly() {
        this.readOnly = true;
    }

    public String getFileName() {
        return this.configPath.getFileName().toString();
    }
}
