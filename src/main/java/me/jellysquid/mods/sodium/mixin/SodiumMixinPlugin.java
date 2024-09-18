package me.jellysquid.mods.sodium.mixin;

import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import me.jellysquid.mods.sodium.client.SodiumPreLaunch;
import net.fabricmc.api.EnvType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.embeddedt.embeddium.asm.AnnotationProcessingEngine;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static me.jellysquid.mods.sodium.client.SodiumClientMod.MODNAME;

@SuppressWarnings("unused")
public class SodiumMixinPlugin implements IMixinConfigPlugin {
    private static final String MIXIN_PACKAGE_ROOT = "me.jellysquid.mods.sodium.mixin.";

    private final Logger logger = LogManager.getLogger(MODNAME);
    private MixinConfig config;

    @Override
    public void onLoad(String mixinPackage) {
        MixinExtrasBootstrap.init();

        try {
            this.config = MixinConfig.load(FabricLoader.getInstance().getConfigDir().resolve("embeddium-mixins.properties").toFile());
        } catch (Exception e) {
            throw new RuntimeException("Could not load configuration file for " + MODNAME, e);
        }

        this.logger.info("Loaded configuration file for " + MODNAME + ": {} options available, {} override(s) found",
                this.config.getOptionCount(), this.config.getOptionOverrideCount());
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String s, String s1) {
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
            String className = baseFolder.relativize(path).toString().replace('/', '.').replace('\\', '.');
            return className.substring(0, className.length() - 6);
        } catch(RuntimeException e) {
            throw new IllegalStateException("Error relativizing " + path + " to " + baseFolder, e);
        }
    }

    private static Path findPathInMod(ModContainer modFile, String... pathComponents) {
        return modFile.findPath(String.join("/", pathComponents)).orElse(null);
    }

    @Override
    public List<String> getMixins() {
        if (FabricLoader.getInstance().getEnvironmentType() != EnvType.CLIENT) {
            return null;
        }

        var modFileOpt = FabricLoader.getInstance().getModContainer("embeddium");

        if (modFileOpt.isEmpty()) {
            // Probably a load error
            logger.error("Could not find embeddium mod, there is likely a dependency error. Skipping mixin application.");
            return null;
        }

        ModContainer modFile = modFileOpt.get();
        Set<Path> rootPaths = new HashSet<>();
        // This allows us to see it from multiple sourcesets if need be
        for(String basePackage : new String[] { "core", "modcompat" }) {
            Path mixinPackagePath = findPathInMod(modFile, "me", "jellysquid", "mods", "sodium", "mixin", basePackage);
            if(mixinPackagePath != null && Files.exists(mixinPackagePath)) {
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
