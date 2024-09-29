package org.embeddedt.embeddium.impl.gui;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.SerializedName;
import com.google.gson.JsonSyntaxException;
import org.embeddedt.embeddium.impl.Embeddium;
import org.embeddedt.embeddium.impl.gui.options.TextProvider;
import net.minecraft.client.GraphicsStatus;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.impl.config.ConfigMigrator;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import static org.embeddedt.embeddium.impl.Embeddium.MODID;

public class EmbeddiumOptions {
    private static final String DEFAULT_FILE_NAME = MODID + "-options.json";

    public final QualitySettings quality = new QualitySettings();
    public final AdvancedSettings advanced = new AdvancedSettings();
    public final PerformanceSettings performance = new PerformanceSettings();
    public final NotificationSettings notifications = new NotificationSettings();

    private boolean readOnly;

    private Path configPath;

    public static EmbeddiumOptions defaults() {
        var options = new EmbeddiumOptions();
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

    public static EmbeddiumOptions load() {
        return load(DEFAULT_FILE_NAME);
    }

    public static EmbeddiumOptions load(String name) {
        Path path = getConfigPath(name);
        EmbeddiumOptions config;
        boolean resaveConfig = true;

        if (Files.exists(path)) {
            try (FileReader reader = new FileReader(path.toFile())) {
                config = GSON.fromJson(reader, EmbeddiumOptions.class);
            } catch (IOException e) {
                throw new RuntimeException("Could not parse config", e);
            } catch (JsonSyntaxException e) {
                Embeddium.logger().error("Could not parse config, will fallback to default settings", e);
                config = new EmbeddiumOptions();
                resaveConfig = false;
            }
        } else {
            config = new EmbeddiumOptions();
        }

        config.configPath = path;

        try {
            if(resaveConfig)
                EmbeddiumOptions.writeToDisk(config);
        } catch (IOException e) {
            throw new RuntimeException("Couldn't update config file", e);
        }

        return config;
    }

    private static Path getConfigPath(String name) {
        return ConfigMigrator.handleConfigMigration(name);
    }

    public static void writeToDisk(EmbeddiumOptions config) throws IOException {
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
