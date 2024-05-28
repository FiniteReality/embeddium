package org.embeddedt.embeddium.gradle.fabric.remapper;

import net.fabricmc.mappingio.MappingReader;
import net.fabricmc.mappingio.MappingUtil;
import net.fabricmc.mappingio.extras.MappingTreeRemapper;
import net.fabricmc.mappingio.format.MappingFormat;
import net.fabricmc.mappingio.tree.MemoryMappingTree;
import net.fabricmc.mappingio.tree.VisitableMappingTree;
import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.jar.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * An artifact transform that deobfuscates intermediary-mapped Fabric mods to Mojmap.
 */
abstract public class RemapperTransform implements TransformAction<RemapperTransform.Parameters> {
    public interface Parameters extends TransformParameters {
        @Input
        Property<String> getMinecraftVersion();

        @InputFile
        @PathSensitive(PathSensitivity.NONE)
        RegularFileProperty getIntermediaryMappings();

        @InputFile
        @PathSensitive(PathSensitivity.NONE)
        RegularFileProperty getMojangMappings();
    }

    @InputArtifact
    protected abstract Provider<FileSystemLocation> getInputArtifact();

    @Override
    public void transform(TransformOutputs outputs) {
        File originalJar = getInputArtifact().get().getAsFile();
        String originalJarName = originalJar.getName();

        if(isFabricMod(originalJar)) {
            try {
                remapToMojmap(originalJar, outputs.file("embeddium_deobf_" + originalJarName));
            } catch(IOException | RuntimeException e) {
                e.printStackTrace();
                throw new RuntimeException("Exception remapping file: " + originalJar, e);
            }
        } else {
            outputs.file(originalJar);
        }
    }

    private void remapToMojmap(File inputJar, File outputJar) throws IOException {
        VisitableMappingTree intermediaryTree = new MemoryMappingTree();
        try(ZipFile intermediaryFile = new ZipFile(getParameters().getIntermediaryMappings().get().getAsFile())) {
            var mappingsEntry = intermediaryFile.getEntry("mappings/mappings.tiny");
            if(mappingsEntry == null) {
                throw new IllegalArgumentException("Cannot find intermediary mappings in jar");
            }
            MappingReader.read(new InputStreamReader(intermediaryFile.getInputStream(mappingsEntry), StandardCharsets.UTF_8), MappingFormat.TINY_2_FILE, intermediaryTree);
        }

        VisitableMappingTree proguardTree = new MemoryMappingTree();
        try(FileInputStream stream = new FileInputStream(getParameters().getMojangMappings().get().getAsFile())) {
            MappingReader.read(new InputStreamReader(stream, StandardCharsets.UTF_8), MappingFormat.PROGUARD_FILE, proguardTree);
        }

        try(InputStream is = new FileInputStream(inputJar)) {
            try(OutputStream os = new FileOutputStream(outputJar)) {
                ModRemapper.remapMod(is, os, new MappingTreeRemapper(intermediaryTree, "intermediary", "official"),
                        new MappingTreeRemapper(proguardTree, MappingUtil.NS_TARGET_FALLBACK, MappingUtil.NS_SOURCE_FALLBACK));
            }
        }
    }

    private boolean isFabricMod(File jar) {
        if(jar.isFile()) {
            try (JarInputStream inputStream =  new JarInputStream(new FileInputStream(jar))) {
                ZipEntry next;
                while ((next = inputStream.getNextEntry()) != null) {
                    if ("fabric.mod.json".equals(next.getName())) {
                        return true;
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
