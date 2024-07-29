package me.jellysquid.mods.sodium.client.compatibility.checks;

import it.unimi.dsi.fastutil.objects.ReferenceLinkedOpenHashSet;
import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.server.packs.FilePackResources;
import net.minecraft.server.packs.PackResources;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class ResourcePackScanner {
    private static final Logger LOGGER = LoggerFactory.getLogger("Sodium-ResourcePackScanner");

    private static final Set<String> SHADER_PROGRAM_BLACKLIST = Set.of(
            "rendertype_solid.vsh",
            "rendertype_solid.fsh",
            "rendertype_solid.json",
            "rendertype_cutout_mipped.vsh",
            "rendertype_cutout_mipped.fsh",
            "rendertype_cutout_mipped.json",
            "rendertype_cutout.vsh",
            "rendertype_cutout.fsh",
            "rendertype_cutout.json",
            "rendertype_translucent.vsh",
            "rendertype_translucent.fsh",
            "rendertype_translucent.json",
            "rendertype_tripwire.vsh",
            "rendertype_tripwire.fsh",
            "rendertype_tripwire.json"
    );

    private static final Set<String> SHADER_INCLUDE_BLACKLIST = Set.of(
            "light.glsl",
            "fog.glsl"
    );

    /**
     * <a href="https://github.com/CaffeineMC/sodium-fabric/issues/1569">#1569</a>
     * Iterate through all active resource packs, and detect resource packs which contain files matching the blacklist.
     * An error message is shown for resource packs which replace terrain core shaders.
     * A warning is shown for resource packs which replace the default light.glsl and fog.glsl shaders.
     * Detailed information on shader files replaced by resource packs is printed in the client log.
     */
    public static void checkIfCoreShaderLoaded(ResourceManager manager) {
        var outputs = manager.listPacks()
                .filter(ResourcePackScanner::isExternalResourcePack)
                .map(ResourcePackScanner::scanResources)
                .toList();

        printToasts(outputs);
        printCompatibilityReport(outputs);
    }

    private static void printToasts(Collection<ScannedResourcePack> resourcePacks) {
        var incompatibleResourcePacks = resourcePacks.stream()
                .filter((pack) -> !pack.shaderPrograms.isEmpty())
                .collect(Collectors.toCollection(ReferenceLinkedOpenHashSet::new));

        var likelyIncompatibleResourcePacks = resourcePacks.stream()
                .filter((pack) -> !pack.shaderIncludes.isEmpty())
                .filter(pack -> !incompatibleResourcePacks.contains(pack))
                .toList();

        boolean shown = false;

        if (!incompatibleResourcePacks.isEmpty()) {
            showConsoleMessage(new TranslatableComponent("sodium.console.core_shaders_error"), MessageLevel.SEVERE);

            for (var entry : incompatibleResourcePacks) {
                showConsoleMessage(new TextComponent(getResourcePackName(entry.resourcePack)), MessageLevel.SEVERE);
            }

            shown = true;
        }

        if (!likelyIncompatibleResourcePacks.isEmpty()) {
            showConsoleMessage(new TranslatableComponent("sodium.console.core_shaders_warn"), MessageLevel.WARN);

            for (var entry : likelyIncompatibleResourcePacks) {
                showConsoleMessage(new TextComponent(getResourcePackName(entry.resourcePack)), MessageLevel.WARN);
            }

            shown = true;
        }

        if (shown) {
            showConsoleMessage(new TranslatableComponent("sodium.console.core_shaders_info"), MessageLevel.INFO);
        }
    }

    private static void printCompatibilityReport(Collection<ScannedResourcePack> scanResults) {
        var builder = new StringBuilder();

        for (var entry : scanResults) {

            if (entry.shaderPrograms.isEmpty() && entry.shaderIncludes.isEmpty()) {
                continue;
            }

            builder.append("- Resource pack: ").append(getResourcePackName(entry.resourcePack)).append("\n");

            if (!entry.shaderPrograms.isEmpty()) {
                emitProblem(builder,
                        "The resource pack replaces terrain shaders, which are not supported",
                        "https://github.com/CaffeineMC/sodium-fabric/wiki/Resource-Packs",
                        entry.shaderPrograms);
            }

            if (!entry.shaderIncludes.isEmpty()) {
                emitProblem(builder,
                        "The resource pack modifies shader include files, which are not fully supported",
                        "https://github.com/CaffeineMC/sodium-fabric/wiki/Resource-Packs",
                        entry.shaderIncludes);
            }
        }

        if (!builder.isEmpty()) {
            LOGGER.error("The following compatibility issues were found with installed resource packs:\n{}", builder);
        }
    }

    private static void emitProblem(StringBuilder builder, String description, String url, List<String> resources) {
        builder.append("\t- Problem found: ").append("\n");
        builder.append("\t\t- Description:\n\t\t\t").append(description).append("\n");
        builder.append("\t\t- More information: ").append(url).append("\n");
        builder.append("\t\t- Files: ").append("\n");

        for (var resource : resources) {
            builder.append("\t\t\t- ").append(resource).append("\n");
        }
    }

    @NotNull
    private static ScannedResourcePack scanResources(PackResources pack) {
        final var ignoredShaders = determineIgnoredShaders(pack);

        if (!ignoredShaders.isEmpty()) {
            LOGGER.warn("Resource pack '{}' indicates the following shaders should be ignored: {}",
                    getResourcePackName(pack), String.join(", ", ignoredShaders));
        }

        final var unsupportedShaderPrograms = new ArrayList<String>();
        final var unsupportedShaderIncludes = new ArrayList<String>();

        pack.getResources(PackType.CLIENT_RESOURCES, "minecraft", "shaders", Integer.MAX_VALUE, p -> true).forEach((identifier) -> {
            // Trim full shader file path to only contain the filename
            final var path = identifier.getPath();
            final var name = path.substring(path.lastIndexOf('/') + 1);

            // Check if the pack has already acknowledged the warnings in this file,
            // in this case we report a different info log about the situation
            if (ignoredShaders.contains(name)) {
                return;
            }

            // Check the path against known problem files
            if (SHADER_PROGRAM_BLACKLIST.contains(name)) {
                unsupportedShaderPrograms.add(path);
            } else if (SHADER_INCLUDE_BLACKLIST.contains(name)) {
                unsupportedShaderIncludes.add(path);
            }
        });

        return new ScannedResourcePack(pack, unsupportedShaderPrograms, unsupportedShaderIncludes);
    }

    private static boolean isExternalResourcePack(PackResources pack) {
        // Embeddium: PathPackResources is used by NeoForge 20.4+, so we will false-detect mods
        // if we check it like upstream. TODO: Investigate a way of detecting PathPackResources instances that
        // aren't from mods
        return pack instanceof FilePackResources;
    }

    private static String getResourcePackName(PackResources pack) {
        var path = pack.getName();

        // Omit 'file/' prefix for the in-game message
        return path.startsWith("file/") ? path.substring(5) : path;
    }

    /**
     * Looks at a resource pack's metadata to find a list of shaders that can be gracefully
     * ignored. This offers resource packs the ability to acknowledge they are shipping shaders
     * which will not work with Sodium, but that Sodium can ignore.
     *
     * @param resourcePack The resource pack to fetch the ignored shaders of
     * @return A list of shaders to ignore, this is the filename only without the path
     */
    private static List<String> determineIgnoredShaders(PackResources resourcePack) {
        var ignoredShaders = new ArrayList<String>();
        try {
            var meta = resourcePack.getMetadataSection(SodiumResourcePackMetadata.SERIALIZER);
            if (meta != null) {
                ignoredShaders.addAll(meta.ignoredShaders());
            }
        } catch (IOException x) {
            LOGGER.error("Failed to load pack.mcmeta file for resource pack '{}'", resourcePack.getName());
        }
        return ignoredShaders;
    }

    private static void showConsoleMessage(MutableComponent message, MessageLevel messageLevel) {
        Console.instance().logMessage(messageLevel, message, 12.5);
    }

    private record ScannedResourcePack(PackResources resourcePack,
                                       ArrayList<String> shaderPrograms,
                                       ArrayList<String> shaderIncludes)
    {

    }
}
