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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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

        @Input
        Property<String> getRemappingCache();
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

    record CachedMappings(MappingTreeRemapper intermediaryToObf, MappingTreeRemapper obfToMojmap, String hash) {}

    record MappingsKey(String intermediaryPath, String mojmapPath) {}

    private static final Map<MappingsKey, CachedMappings> mappingsByVersion = new ConcurrentHashMap<>();

    private void remapToMojmap(File inputJar, File outputJar) throws IOException {
        var intermediaryJar = getParameters().getIntermediaryMappings().get().getAsFile();
        var mojmapFile = getParameters().getMojangMappings().get().getAsFile();
        var cachedMappings = mappingsByVersion.computeIfAbsent(new MappingsKey(intermediaryJar.getAbsolutePath(), mojmapFile.getAbsolutePath()), key -> {
            try {
                MemoryMappingTree intermediaryTree = new MemoryMappingTree();
                intermediaryTree.setIndexByDstNames(true);
                MessageDigest digest = MessageDigest.getInstance("MD5");
                digest.reset();
                try(ZipFile intermediaryFile = new ZipFile(getParameters().getIntermediaryMappings().get().getAsFile())) {
                    var mappingsEntry = intermediaryFile.getEntry("mappings/mappings.tiny");
                    if(mappingsEntry == null) {
                        throw new IllegalArgumentException("Cannot find intermediary mappings in jar");
                    }
                    MappingReader.read(new InputStreamReader(new DigestInputStream(intermediaryFile.getInputStream(mappingsEntry), digest), StandardCharsets.UTF_8), MappingFormat.TINY_2_FILE, intermediaryTree);
                }

                MemoryMappingTree proguardTree = new MemoryMappingTree();
                proguardTree.setIndexByDstNames(true);
                try(FileInputStream stream = new FileInputStream(getParameters().getMojangMappings().get().getAsFile())) {
                    MappingReader.read(new InputStreamReader(new DigestInputStream(stream, digest), StandardCharsets.UTF_8), MappingFormat.PROGUARD_FILE, proguardTree);
                }

                StringBuilder hashBuilder = new StringBuilder();

                for(byte b : digest.digest()) {
                    hashBuilder.append(String.format("%02x", b));
                }

                return new CachedMappings(
                        new MappingTreeRemapper(intermediaryTree, "intermediary", "official"),
                        new MappingTreeRemapper(proguardTree, MappingUtil.NS_TARGET_FALLBACK, MappingUtil.NS_SOURCE_FALLBACK),
                        hashBuilder.toString()
                );
            } catch(IOException | NoSuchAlgorithmException e) {
                throw new RuntimeException("Failure to build mappings trees", e);
            }
        });

        File cacheDir = new File(getParameters().getRemappingCache().get() + File.separator + cachedMappings.hash());
        if(!cacheDir.exists()) {
            cacheDir.mkdirs();
        }

        File outputCachedJar = new File(cacheDir, outputJar.getName());

        if(!outputCachedJar.exists()) {
            System.out.println("Remapping " + outputCachedJar.getName());
            try(InputStream is = new FileInputStream(inputJar)) {
                try(OutputStream os = new FileOutputStream(outputCachedJar)) {
                    ModRemapper.remapMod(is, os, cachedMappings.intermediaryToObf(), cachedMappings.obfToMojmap());
                }
            }
        }

        outputJar.delete();
        Files.copy(outputCachedJar.toPath(), outputJar.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.COPY_ATTRIBUTES);
    }

    private boolean isFabricMod(File jar) {
        if(jar.isFile()) {
            try (ZipFile zf = new ZipFile(jar)) {
                return zf.getEntry("fabric.mod.json") != null;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }
}
