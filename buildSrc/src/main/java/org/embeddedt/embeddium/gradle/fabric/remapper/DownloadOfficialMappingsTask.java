package org.embeddedt.embeddium.gradle.fabric.remapper;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class DownloadOfficialMappingsTask {

    public static void run(String targetVersion, File outputFile) throws IOException {
        URL url = new URL("https://launchermeta.mojang.com/mc/game/version_manifest_v2.json");
        JsonObject versionManifest;
        try(InputStream stream = url.openStream()) {
           versionManifest = (JsonObject)JsonParser.parseReader(new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
        System.out.println("Downloaded launcher manifest.");
        var versionArray = versionManifest.getAsJsonArray("versions");
        URL targetURL = null;
        for(var element : versionArray) {
            if(element.getAsJsonObject().get("id").getAsJsonPrimitive().getAsString().equals(targetVersion)) {
                targetURL = new URL(element.getAsJsonObject().get("url").getAsJsonPrimitive().getAsString());
                break;
            }
        }
        if(targetURL == null) {
            throw new IllegalArgumentException("No version info found for " + targetVersion);
        }
        JsonObject targetManifest;
        try(InputStream stream = targetURL.openStream()) {
            targetManifest = (JsonObject)JsonParser.parseReader(new JsonReader(new InputStreamReader(stream, StandardCharsets.UTF_8)));
        }
        System.out.println("Downloaded " + targetVersion + " manifest.");
        URL mappingsURL = new URL(targetManifest.getAsJsonObject("downloads").getAsJsonObject("client_mappings").getAsJsonPrimitive("url").getAsString());
        try(InputStream is = mappingsURL.openStream()) {
            byte[] mappings = is.readAllBytes();
            System.out.println("Downloaded " + targetVersion + " mappings.");
            // TODO: validate mappings SHA
            try(FileOutputStream fos = new FileOutputStream(outputFile)) {
                fos.write(mappings);
            }
        }
    }
}
