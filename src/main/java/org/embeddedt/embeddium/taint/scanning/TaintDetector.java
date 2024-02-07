package org.embeddedt.embeddium.taint.scanning;


import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import me.jellysquid.mods.sodium.mixin.MixinClassValidator;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.moddiscovery.ModFile;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import net.minecraftforge.fml.loading.moddiscovery.ModInfo;
import net.minecraftforge.forgespi.language.IModInfo;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.objectweb.asm.tree.ClassNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class TaintDetector {
    private static final String TAINT_MODE = System.getProperty("embeddium.taint_scan");
    private static final Logger LOGGER = LogManager.getLogger("Embeddium-TaintDetector");

    private static final ClassConstantPoolParser PARSER = new ClassConstantPoolParser(
            "me/jellysquid/mods/sodium"
    );

    private static final Multimap<ModFileInfo, TargetingClass> DISCOVERED_MODS = ArrayListMultimap.create();

    private static final Set<String> EXCLUDED_MOD_IDS = ImmutableSet.of("embeddium");

    static class TargetingClass {
        String className;
        boolean isMixin;
    }

    public static void init() {
        if(!Objects.equals(TAINT_MODE, "true")) {
            return;
        }

        Executor taintScanner = Executors.newSingleThreadExecutor(task -> {
           Thread worker = new Thread(task, "Embeddium Mod Analyzer");
           worker.setPriority(Thread.MIN_PRIORITY);
           worker.setDaemon(true);
           return worker;
        });

        CompletableFuture.runAsync(() -> {
            Stopwatch watch = Stopwatch.createStarted();
            LOGGER.info("Scanning for mods that depend on Embeddium code...");
            scanMods();
            watch.stop();
            LOGGER.info("Finished scanning mods in {}", watch);
            presentResults();
        }, taintScanner);
    }

    private static void scanMods() {
        List<Path> classPaths = new ArrayList<>();
        ModFileInfo self = FMLLoader.getLoadingModList().getModFileById("embeddium");
        Objects.requireNonNull(self, "Embeddium mod file does not exist");
        for (ModFileInfo file : FMLLoader.getLoadingModList().getModFiles()) {
            // Skip scanning files that provide a mod we know and trust
            if (file.getMods().stream().anyMatch(modInfo -> EXCLUDED_MOD_IDS.contains(modInfo.getModId()))) {
                continue;
            }

            classPaths.clear();
            file.getFile().scanFile(path -> {
                if(path.getFileName().toString().endsWith(".class")) {
                   classPaths.add(path);
                }
            });
            for(Path path : classPaths) {
                try {
                    TargetingClass clz = checkClass(path);
                    if(clz != null) {
                        DISCOVERED_MODS.put(file, clz);
                    }
                } catch(IOException | RuntimeException e) {
                    LOGGER.error("An error occured scanning class {}, it will be skipped: {}", path, e);
                }
            }
        }
    }

    private static TargetingClass checkClass(Path path) throws IOException {
        byte[] bytecode = Files.readAllBytes(path);

        // Nothing to do if it does not reference us in a constant pool entry
        if(!PARSER.find(bytecode, true)) {
            return null;
        }

        ClassNode node = MixinClassValidator.fromBytecode(bytecode);

        TargetingClass targetingClass = new TargetingClass();
        targetingClass.className = node.name;
        targetingClass.isMixin = MixinClassValidator.isMixinClass(node);

        return targetingClass;
    }

    private static void presentResults() {
        if(DISCOVERED_MODS.isEmpty()) {
            return;
        }

        StringBuilder theResults = new StringBuilder();
        theResults.append(DISCOVERED_MODS.keySet().size()).append(" mods were found that reference Embeddium internals:\n");
        DISCOVERED_MODS.asMap().forEach((file, listClass) -> {
            theResults
                    .append("Mod file '").append(file.getFile().getFileName())
                    .append("' providing mods [")
                    .append(file.getMods().stream().map(IModInfo::getModId).collect(Collectors.joining(", ")))
                    .append("] with ")
                    .append(listClass.size())
                    .append(" classes\n");

            Map<Boolean, List<TargetingClass>> partitionedClasses = listClass.stream().collect(Collectors.partitioningBy(t -> t.isMixin));
            for(Map.Entry<Boolean, List<TargetingClass>> partition : partitionedClasses.entrySet()) {
                if(partition.getValue().isEmpty()) {
                    continue;
                }

                theResults.append("|-- ");
                theResults.append(partition.getKey() ? "mixin " : "non-mixin ");
                theResults.append("\n");
                for(TargetingClass targetingClass : partition.getValue()) {
                    theResults.append("    |-- ");
                    theResults.append(targetingClass.className);
                    theResults.append('\n');
                }
            }
        });

        LOGGER.info(theResults);
    }
}
