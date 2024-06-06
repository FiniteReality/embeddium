package org.embeddedt.embeddium.gradle.fabric.remapper;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.plugins.JavaPlugin;

import java.io.IOException;

/**
 * Entry point of our plugin that should be applied in the root project.
 */
public class RemapperPlugin implements Plugin<Project> {
    @Override
    public void apply(Project project) {
        // setup the transform for all projects in the build
        project.getPlugins().withType(JavaPlugin.class).configureEach(javaPlugin -> configureTransform(project));
    }

    private void configureTransform(Project project) {
        Attribute<String> artifactType = Attribute.of("artifactType", String.class);
        Attribute<Boolean> devCompatible = Attribute.of("isEmbeddiumDevCompatibleMod", Boolean.class);

        var intermediaryConfig = project.getConfigurations().create("embeddiumFabricIntermediary");
        var minecraftVersion = (String)project.getProperties().get("minecraft_version");

        project.getDependencies().add(intermediaryConfig.getName(), "net.fabricmc:intermediary:" + minecraftVersion + ":v2");

        // Make sure the build directory exists
        project.getLayout().getBuildDirectory().get().getAsFile().mkdirs();

        var mappingsFile = project.getLayout().getBuildDirectory().file("embeddium_mappings_" + minecraftVersion + ".txt").get().getAsFile();

        if(!mappingsFile.exists()) {
            try {
                DownloadOfficialMappingsTask.run(minecraftVersion, mappingsFile);
            } catch(IOException e) {
                throw new RuntimeException("Failed to download mappings", e);
            }
        }

        // compile and runtime classpath express that they only accept modules by requesting the isEmbeddiumDevCompatibleMod=true attribute
        project.getConfigurations().matching(this::isResolvingJavaPluginConfiguration).all(c -> c.getAttributes().attribute(devCompatible, true));

        // all Jars have a isEmbeddiumDevCompatibleMod=false attribute by default; the transform also recognizes non-Fabric mods and returns them without modification
        project.getDependencies().getArtifactTypes().getByName("jar").getAttributes().attribute(devCompatible, false);

        // register the transform for Jars and "isEmbeddiumDevCompatibleMod=false -> isEmbeddiumDevCompatibleMod=true"; the plugin extension object fills the input parameter
        project.getDependencies().registerTransform(RemapperTransform.class, t -> {
            t.getParameters().getMinecraftVersion().set(minecraftVersion);
            t.getParameters().getIntermediaryMappings().fileProvider(project.provider(() -> {
                var deps = intermediaryConfig.resolve();
                if(deps.isEmpty()) {
                    throw new IllegalStateException("No intermediary mappings found");
                }
                return deps.iterator().next();
            }));
            t.getParameters().getMojangMappings().set(mappingsFile);
            t.getParameters().getRemappingCache().set(project.getLayout().getBuildDirectory().dir("embeddium_remap_cache").get().getAsFile().getAbsolutePath());
            t.getFrom().attribute(artifactType, "jar").attribute(devCompatible, false);
            t.getTo().attribute(artifactType, "jar").attribute(devCompatible, true);
        });
    }

    private boolean isResolvingJavaPluginConfiguration(Configuration configuration) {
        if (!configuration.isCanBeResolved()) {
            return false;
        }
        return configuration.getName().endsWith(JavaPlugin.COMPILE_CLASSPATH_CONFIGURATION_NAME.substring(1))
                || configuration.getName().endsWith(JavaPlugin.RUNTIME_CLASSPATH_CONFIGURATION_NAME.substring(1))
                || configuration.getName().endsWith(JavaPlugin.ANNOTATION_PROCESSOR_CONFIGURATION_NAME.substring(1));
    }
}
