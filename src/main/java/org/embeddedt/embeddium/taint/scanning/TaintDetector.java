package org.embeddedt.embeddium.taint.scanning;


import com.google.common.base.Stopwatch;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import me.jellysquid.mods.sodium.mixin.MixinClassValidator;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
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
import java.util.stream.Stream;

public class TaintDetector {
    private static final String TAINT_MODE = System.getProperty("embeddium.taint_scan");
    private static final Logger LOGGER = LogManager.getLogger("Embeddium-TaintDetector");

    private static final ClassConstantPoolParser PARSER = new ClassConstantPoolParser(
            "me/jellysquid/mods/sodium"
    );

    private static final Multimap<ModContainer, TargetingClass> DISCOVERED_MODS = ArrayListMultimap.create();

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
        ModContainer self = FabricLoader.getInstance().getModContainer("embeddium").orElseThrow();
        Objects.requireNonNull(self, "Embeddium mod file does not exist");
        for (ModContainer file : FabricLoader.getInstance().getAllMods()) {
            // Skip scanning files that provide a mod we know and trust
            if (EXCLUDED_MOD_IDS.contains(file.getMetadata().getId())) {
                continue;
            }

            classPaths.clear();
            file.getRootPaths().forEach(rootPath -> {
                try(Stream<Path> pathStream = Files.find(rootPath, Integer.MAX_VALUE, (p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith("class"))) {
                    pathStream.forEach(classPaths::add);
                } catch(IOException e) {
                    LOGGER.error("Exception scanning root path {}", rootPath, e);
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
                    .append("Mod file '").append(file.getOrigin().toString())
                    .append("' providing mods [")
                    .append(file.getMetadata().getId())
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
