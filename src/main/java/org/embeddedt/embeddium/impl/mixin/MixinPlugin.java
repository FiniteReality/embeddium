package org.embeddedt.embeddium.impl.mixin;

import org.embeddedt.embeddium.impl.EmbeddiumPreLaunch;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.fml.loading.moddiscovery.ModFile;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.impl.asm.AnnotationProcessingEngine;
import org.embeddedt.embeddium.impl.config.ConfigMigrator;
import org.embeddedt.embeddium_integrity.MixinTaintDetector;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.embeddedt.embeddium.impl.Embeddium.MODNAME;

@SuppressWarnings("unused")
public class MixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE_ROOT = "org.embeddedt.embeddium.impl.mixin.";

    private final Logger logger = LogManager.getLogger(MODNAME);
    private MixinConfig config;

    // TODO handle production
    private static final boolean RUNNING_ON_FABRIC;

    static {
        boolean mlPresent;
        try {
            Class.forName("cpw.mods.modlauncher.Launcher", false, MixinPlugin.class.getClassLoader());
            // TODO write proper reflection-based fabric detector that won't see Connector
            String mixinService = System.getProperty("mixin.service");
            mlPresent = mixinService == null || !mixinService.contains("MixinServiceKnot");
        } catch(ReflectiveOperationException e) {
            mlPresent = false;
        }

        RUNNING_ON_FABRIC = !mlPresent;
    }

    private static final Set<String> BLACKLISTED_MIXINS = !RUNNING_ON_FABRIC ? Set.of() : Set.of(
            "features.render.model.ChunkRenderTypeSetMixin"
    );

    @Override
    public void onLoad(String mixinPackage) {
        try {
            this.config = MixinConfig.load(ConfigMigrator.handleConfigMigration("embeddium-mixins.properties").toFile());
        } catch (Exception e) {
            throw new RuntimeException("Could not load configuration file for " + MODNAME, e);
        }

        this.logger.info("Loaded configuration file for " + MODNAME + ": {} options available, {} override(s) found",
                this.config.getOptionCount(), this.config.getOptionOverrideCount());

        EmbeddiumPreLaunch.onPreLaunch();

        MixinTaintDetector.initialize();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return true;
    }

    private boolean isMixinEnabled(String mixin) {
        MixinOption option = this.config.getEffectiveOptionForMixin(mixin);

        if (option == null) {
            // Missing modcompat options are fine
            if(!mixin.startsWith("modcompat.")) {
                this.logger.error("No rules matched mixin '{}', treating as foreign and disabling!", mixin);
            }

            return false;
        }

        if (option.isOverridden()) {
            String source = "[unknown]";

            if (option.isUserDefined()) {
                source = "user configuration";
            } else if (option.isModDefined()) {
                source = "mods [" + String.join(", ", option.getDefiningMods()) + "]";
            }

            if (option.isEnabled()) {
                this.logger.warn("Force-enabling mixin '{}' as rule '{}' (added by {}) enables it", mixin,
                        option.getName(), source);
            } else {
                this.logger.warn("Force-disabling mixin '{}' as rule '{}' (added by {}) disables it and children", mixin,
                        option.getName(), source);
            }
        }

        return option.isEnabled();
    }
    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    private static String mixinClassify(Path baseFolder, Path path) {
        try {
            String className = baseFolder.relativize(path).toString().replace('/', '.');
            return className.substring(0, className.length() - 6);
        } catch(RuntimeException e) {
            throw new IllegalStateException("Error relativizing " + path + " to " + baseFolder, e);
        }
    }

    @Override
    public List<String> getMixins() {
        if (FMLLoader.getDist() != Dist.CLIENT) {
            return null;
        }

        ModFileInfo modFileInfo = FMLLoader.getLoadingModList().getModFileById("embeddium");

        if (modFileInfo == null) {
            // Probably a load error
            logger.error("Could not find embeddium mod, there is likely a dependency error. Skipping mixin application.");
            return null;
        }

        ModFile modFile = modFileInfo.getFile();
        Set<Path> rootPaths = new HashSet<>();
        // This allows us to see it from multiple sourcesets if need be
        for(String basePackage : new String[] { "core", "modcompat", "fabric" }) {
            Path mixinPackagePath = modFile.findResource("org", "embeddedt", "embeddium", "impl", "mixin", basePackage);
            if(Files.exists(mixinPackagePath)) {
                rootPaths.add(mixinPackagePath.getParent().toAbsolutePath());
            }
        }

        Set<String> possibleMixinClasses = new HashSet<>();
        for(Path rootPath : rootPaths) {
            try(Stream<Path> mixinStream = Files.find(rootPath, Integer.MAX_VALUE, (path, attrs) -> attrs.isRegularFile() && path.getFileName().toString().endsWith(".class"))) {
                mixinStream
                        .map(Path::toAbsolutePath)
                        .filter(MixinClassValidator::isMixinClass)
                        .map(path -> mixinClassify(rootPath, path))
                        .filter(this::isMixinEnabled)
                        .forEach(possibleMixinClasses::add);
            } catch(IOException e) {
                e.printStackTrace();
            }
        }

        possibleMixinClasses.removeAll(BLACKLISTED_MIXINS);

        return new ArrayList<>(possibleMixinClasses);
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
        if(targetClassName.startsWith("org.embeddedt.embeddium.") || targetClassName.startsWith("me.jellysquid.mods.sodium.")) {
            AnnotationProcessingEngine.processClass(targetClass);
        }
    }
}
