package org.embeddedt.embeddium.taint.incompats;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;
import net.fabricmc.loader.impl.gui.FabricGuiEntry;
import net.fabricmc.loader.impl.gui.FabricStatusTree;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

/**
 * Check if Sodium or Indium are in the mods folder, and refuse to run if so.
 */
public class SupersededModDetector implements PreLaunchEntrypoint {
    private record SupersededMod(String modId, String potentialClass) {}
    private static final List<SupersededMod> POSSIBLE_MODS = ImmutableList.of(
            new SupersededMod("sodium", "me/jellysquid/mods/sodium/client/SodiumClientMod.class"),
            new SupersededMod("indium", "link/infra/indium/Indium.class")
    );

    @Override
    public void onPreLaunch() {
        var supersededMods = getSupersededModFiles();

        if (supersededMods.isEmpty()) {
            return;
        }

        FabricGuiEntry.displayError("Incompatible mods!", null, tree -> {
            FabricStatusTree.FabricStatusTab crashTab = tree.addTab("Error");
            crashTab.node.addMessage("Embeddium found one or mods installed that it replaces. These other mods should be removed:", FabricStatusTree.FabricTreeWarningLevel.ERROR);
            supersededMods.forEach(name -> crashTab.node.addMessage(name, FabricStatusTree.FabricTreeWarningLevel.ERROR));
            tree.tabs.removeIf(tab -> tab != crashTab);
        }, true);
    }

    private List<String> getSupersededModFiles() {
        // We only care about the very common case (Sodium or Indium remaining in the mods folder after migration).
        // As such, we only scan mods/.
        Path modsFolder = FabricLoader.getInstance().getGameDir().resolve("mods");

        try(Stream<Path> modFileStream = Files.find(modsFolder, 1, (p, a) -> a.isRegularFile() && p.getFileName().toString().endsWith(".jar"))) {
            return modFileStream.filter(path -> {
                try(ZipFile zf = new ZipFile(path.toFile())) {
                    var modJson = zf.getEntry("fabric.mod.json");
                    if (modJson == null) {
                        return false; // not a Fabric mod?
                    }
                    JsonElement rootElement = JsonParser.parseReader(new JsonReader(new InputStreamReader(zf.getInputStream(modJson))));
                    if(!rootElement.isJsonObject()) {
                        return false; // unknown format
                    }
                    var idP = ((JsonObject)rootElement).getAsJsonPrimitive("id");
                    if(!idP.isString()) {
                        return false;
                    }
                    var id = idP.getAsString();
                    for(SupersededMod candidate : POSSIBLE_MODS) {
                        if(candidate.modId.equals(id)) {
                            if(zf.getEntry(candidate.potentialClass) != null) {
                                return true;
                            } else {
                                System.err.println("Hmm, found mod file claiming to be " + candidate.modId + " but it is missing class");
                            }
                        }
                    }
                } catch(ZipException | JsonParseException ignored) {
                    // probably not a mod file
                } catch(IOException e) {
                    e.printStackTrace();
                }
                // Did not match any candidate
                return false;
            }).map(p -> p.getFileName().toString()).collect(Collectors.toList());
        } catch(IOException e) {
            e.printStackTrace();
            return List.of();
        }
    }
}
