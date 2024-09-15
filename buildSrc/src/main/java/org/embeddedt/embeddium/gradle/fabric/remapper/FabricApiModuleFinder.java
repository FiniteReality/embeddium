package org.embeddedt.embeddium.gradle.fabric.remapper;

import java.io.*;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.google.common.io.ByteStreams;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Dependency;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Based on code from Fabric Loom, used under the terms of the MIT License.
 */
public abstract class FabricApiModuleFinder {
    @Inject
    public abstract Project getProject();

    private static final HashMap<String, Map<String, String>> moduleVersionCache = new HashMap<>();
    private static final HashMap<String, Map<String, String>> deprecatedModuleVersionCache = new HashMap<>();

    public Dependency module(String moduleName, String fabricApiVersion) {
        return getProject().getDependencies()
                .create(getDependencyNotation(moduleName, fabricApiVersion));
    }

    public String moduleVersion(String moduleName, String fabricApiVersion) {
        String moduleVersion = moduleVersionCache
                .computeIfAbsent(fabricApiVersion, this::getApiModuleVersions)
                .get(moduleName);

        if (moduleVersion == null) {
            moduleVersion = deprecatedModuleVersionCache
                    .computeIfAbsent(fabricApiVersion, this::getDeprecatedApiModuleVersions)
                    .get(moduleName);
        }

        if (moduleVersion == null) {
            throw new RuntimeException("Failed to find module version for module: " + moduleName);
        }

        return moduleVersion;
    }

    private String getDependencyNotation(String moduleName, String fabricApiVersion) {
        return String.format("net.fabricmc.fabric-api:%s:%s", moduleName, moduleVersion(moduleName, fabricApiVersion));
    }

    private Map<String, String> getApiModuleVersions(String fabricApiVersion) {
        try {
            return populateModuleVersionMap(getApiMavenPom(fabricApiVersion));
        } catch (PomNotFoundException e) {
            throw new RuntimeException("Could not find fabric-api version: " + fabricApiVersion);
        }
    }

    private Map<String, String> getDeprecatedApiModuleVersions(String fabricApiVersion) {
        try {
            return populateModuleVersionMap(getDeprecatedApiMavenPom(fabricApiVersion));
        } catch (PomNotFoundException e) {
            // Not all fabric-api versions have deprecated modules, return an empty map to cache this fact.
            return Collections.emptyMap();
        }
    }

    private Map<String, String> populateModuleVersionMap(File pomFile) {
        try {
            DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
            Document pom = docBuilder.parse(pomFile);

            Map<String, String> versionMap = new HashMap<>();

            NodeList dependencies = ((Element) pom.getElementsByTagName("dependencies").item(0)).getElementsByTagName("dependency");

            for (int i = 0; i < dependencies.getLength(); i++) {
                Element dep = (Element) dependencies.item(i);
                Element artifact = (Element) dep.getElementsByTagName("artifactId").item(0);
                Element version = (Element) dep.getElementsByTagName("version").item(0);

                if (artifact == null || version == null) {
                    throw new RuntimeException("Failed to find artifact or version");
                }

                versionMap.put(artifact.getTextContent(), version.getTextContent());
            }

            return versionMap;
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse " + pomFile.getName(), e);
        }
    }

    private File getApiMavenPom(String fabricApiVersion) throws PomNotFoundException {
        return getPom("fabric-api", fabricApiVersion);
    }

    private File getDeprecatedApiMavenPom(String fabricApiVersion) throws PomNotFoundException {
        return getPom("fabric-api-deprecated", fabricApiVersion);
    }

    private File getPom(String name, String version) throws PomNotFoundException {
        final var mavenPom = new File(getProject().getLayout().getBuildDirectory().getAsFile().get(), "fabric-api/%s-%s.pom".formatted(name, version));

        if(!mavenPom.exists()) {
            mavenPom.getParentFile().mkdirs();
            try(FileOutputStream fos = new FileOutputStream(mavenPom)) {
                URL url = new URL(String.format("https://maven.fabricmc.net/net/fabricmc/fabric-api/%2$s/%1$s/%2$s-%1$s.pom", version, name));
                try(InputStream stream = url.openStream()) {
                    ByteStreams.copy(stream, fos);
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Failed to download maven info to " + mavenPom.getName(), e);
            }
        }

        return mavenPom;
    }

    private static class PomNotFoundException extends Exception {
        PomNotFoundException(Throwable cause) {
            super(cause);
        }
    }
}
