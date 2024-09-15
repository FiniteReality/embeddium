package org.embeddedt.embeddium.gradle.fabric.remapper;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ExternalModuleDependency;
import org.gradle.api.artifacts.FileCollectionDependency;
import org.gradle.api.artifacts.ProjectDependency;
import org.gradle.api.artifacts.type.ArtifactTypeDefinition;
import org.gradle.api.attributes.Attribute;
import org.gradle.api.plugins.JavaPlugin;

import java.io.IOException;

/**
 * Entry point of our plugin that should be applied in the root project.
 */
public class RemapperPlugin implements Plugin<Project> {
    private static final Attribute<Boolean> DEV_COMPATIBLE = Attribute.of("isEmbeddiumDevCompatibleMod", Boolean.class);

    @Override
    public void apply(Project project) {
        // setup the transform for all projects in the build
        project.getPlugins().withType(JavaPlugin.class).configureEach(javaPlugin -> configureTransform(project));
    }

    private Configuration createRemapConfig(Project project, String name, Configuration parent) {
        var config = project.getConfigurations().create(name, spec -> {
            // Require isEmbeddiumDevCompatibleMod
            spec.attributes(attr -> attr.attribute(DEV_COMPATIBLE, true));

            // Magic borrowed from MDG
            spec.withDependencies(dependencies -> dependencies.forEach(dep -> {
                if (dep instanceof ExternalModuleDependency externalModuleDependency) {
                    project.getDependencies().constraints(constraints -> {
                        constraints.add(parent.getName(), externalModuleDependency.getGroup() + ":" + externalModuleDependency.getName() + ":" + externalModuleDependency.getVersion(), c -> {
                            c.attributes(a -> a.attribute(DEV_COMPATIBLE, true));
                        });
                    });
                } else if (dep instanceof FileCollectionDependency fileCollectionDependency) {
                    project.getDependencies().constraints(constraints -> {
                        constraints.add(parent.getName(), fileCollectionDependency.getFiles(), c -> {
                            c.attributes(a -> a.attribute(DEV_COMPATIBLE, true));
                        });
                    });
                } else if (dep instanceof ProjectDependency projectDependency) {
                    project.getDependencies().constraints(constraints -> {
                        constraints.add(parent.getName(), projectDependency.getDependencyProject(), c -> {
                            c.attributes(a -> a.attribute(DEV_COMPATIBLE, true));
                        });
                    });
                }
            }));
        });
        parent.extendsFrom(config);
        return config;
    }

    private void configureTransform(Project project) {
        Attribute<String> artifactType = Attribute.of("artifactType", String.class);

        var intermediaryConfig = project.getConfigurations().create("embeddiumFabricIntermediary");
        var minecraftVersion = (String)project.getProperties().get("minecraft_version");

        project.getDependencies().add(intermediaryConfig.getName(), "net.fabricmc:intermediary:" + minecraftVersion + ":v2");

        project.getExtensions().create("fabricApiModuleFinder", FabricApiModuleFinder.class);

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

        createRemapConfig(project, "fabricCompileOnly", project.getConfigurations().getByName("compileOnly"));

        // all Jars have a isEmbeddiumDevCompatibleMod=false attribute by default; the transform also recognizes non-Fabric mods and returns them without modification
        project.getDependencies().attributesSchema(schema -> schema.attribute(DEV_COMPATIBLE));
        project.getDependencies().getArtifactTypes().named("jar", t -> t.getAttributes().attribute(DEV_COMPATIBLE, false));

        // register the transform for Jars and "isEmbeddiumDevCompatibleMod=false -> isEmbeddiumDevCompatibleMod=true"; the plugin extension object fills the input parameter
        project.getDependencies().registerTransform(RemapperTransform.class, t -> {
            t.parameters(params -> {
                params.getMinecraftVersion().set(minecraftVersion);
                params.getIntermediaryMappings().fileProvider(project.provider(() -> {
                    var deps = intermediaryConfig.resolve();
                    if(deps.isEmpty()) {
                        throw new IllegalStateException("No intermediary mappings found");
                    }
                    return deps.iterator().next();
                }));
                params.getMojangMappings().set(mappingsFile);
                params.getRemappingCache().set(project.getLayout().getBuildDirectory().dir("embeddium_remap_cache").get().getAsFile().getAbsolutePath());
            });
            t.getFrom().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE).attribute(DEV_COMPATIBLE, false);
            t.getTo().attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, ArtifactTypeDefinition.JAR_TYPE).attribute(DEV_COMPATIBLE, true);
        });
    }
}
