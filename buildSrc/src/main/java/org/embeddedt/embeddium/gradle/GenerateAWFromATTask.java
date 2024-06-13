package org.embeddedt.embeddium.gradle;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.util.service.ScopedSharedServiceManager;
import net.fabricmc.loom.util.service.SharedServiceManager;
import net.fabricmc.mappingio.tree.MappingTree;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.*;

import java.io.*;
import java.nio.file.Files;

// Code originally by Nolij, used with permission & modified for Embeddium
public abstract class GenerateAWFromATTask extends DefaultTask {
    @OutputFile
    public abstract RegularFileProperty getAccessWidenerPath();

    @InputFile
    public abstract RegularFileProperty getAccessTransformerPath();

    @Input
    @Optional
    public abstract Property<Boolean> getOverwriteAccessWidener();

    private void runGeneration(SharedServiceManager serviceManager) throws IOException {
        if (!getAccessTransformerPath().isPresent())
            throw new AssertionError("accessTransformerPath not set in build.gradle!");
        else if (!getAccessWidenerPath().isPresent())
            throw new AssertionError("accessWidenerPath not set in build.gradle!");

        final boolean overwriteWidener = getOverwriteAccessWidener().getOrElse(false);

        final var mappings = LoomGradleExtension.get(getProject()).getMappingConfiguration().getMappingsService(serviceManager).getMappingTree();
        mappings.setIndexByDstNames(true);

        final int namedNsId = mappings.getNamespaceId("named");
        // we don't actually need to remap for loom
        final int intermediaryNsId = namedNsId;

        final File widenerFile = getAccessWidenerPath().get().getAsFile();
        final File transformerFile = getAccessTransformerPath().get().getAsFile();

        final File tempFile = Files.createTempFile("embeddium", ".accesswidener").toFile();
        tempFile.deleteOnExit();

        final BufferedReader widenerReader;
        final BufferedReader transformerReader = new BufferedReader(new FileReader(transformerFile));
        final BufferedWriter tempWriter = new BufferedWriter(new FileWriter(tempFile));

        String line;

        if (overwriteWidener) {
            widenerReader = null;

            tempWriter.write("accessWidener v2 " + mappings.getNamespaceName(intermediaryNsId));
            tempWriter.newLine();
            tempWriter.newLine();
            tempWriter.write("# embeddium {");
            tempWriter.newLine();
        } else {
            widenerReader = new BufferedReader(new FileReader(widenerFile));

            while ((line = widenerReader.readLine()) != null) {
                tempWriter.write(line);
                tempWriter.newLine();
                if (line.matches("^\\s*#\\s*embeddium\\s*\\{\\s*$"))
                    break;
            }
            if (line == null)
                throw new AssertionError("No \"# embeddium {\" block found!");
        }

        while ((line = transformerReader.readLine()) != null) {
            if (line.startsWith("#")) { // keep AT comments
                tempWriter.write(line);
                tempWriter.newLine();

                continue;
            } else if (line.isBlank()) {
                continue;
            }

            tempWriter.write("# "); // insert AT line for reference and debugging
            tempWriter.write(line);
            tempWriter.newLine();

            // TODO: rewrite below logic to take existing state
            //  in to account (right now it's potentially wasteful)

            final String content;
            final int endParseIndex = line.indexOf('#');
            if (endParseIndex == -1)
                content = line.strip();
            else
                content = line.substring(0, endParseIndex).strip();

            final String[] tokens = content.split("\\s+");

            final int finalModIndex = tokens[0].length() - 2;
            final String visibility;
            final boolean unfinal;
            if ((unfinal = tokens[0].endsWith("-f")) || tokens[0].endsWith("+f")) {
                visibility = tokens[0].substring(0, finalModIndex);
            } else {
                visibility = tokens[0];
            }

            final String mojangClassName = tokens[1].replaceAll("\\.", "/");
            final MappingTree.ClassMapping classMapping = mappings.getClass(mojangClassName, namedNsId);

            if(classMapping == null) {
                throw new RuntimeException("Missing class mapping for " + mojangClassName);
            }

            final String className = classMapping.getName(intermediaryNsId);

            if (tokens.length == 2) { // target is a class; cease parsing
                tempWriter.write("transitive-");
                if (unfinal)
                    tempWriter.write("extendable ");
                else if (!visibility.equals("private"))
                    tempWriter.write("accessible ");

                tempWriter.write("class ");
                tempWriter.write(className);
                tempWriter.newLine();

                continue;
            }

            final int methodDescIndex = tokens[2].indexOf('(');
            if (methodDescIndex == -1) { // field
                final MappingTree.FieldMapping fieldMapping = classMapping.getField(tokens[2], null, namedNsId);

                if(fieldMapping == null) {
                    throw new RuntimeException("Missing field mapping for " + tokens[2]);
                }

                final String mappedName = fieldMapping.getName(intermediaryNsId);
                final String mappedDesc = fieldMapping.getDesc(intermediaryNsId);

                final String suffix = " field %s %s %s".formatted(className, mappedName, mappedDesc);

                if (unfinal) {
                    tempWriter.write("transitive-mutable");
                    tempWriter.write(suffix);
                    tempWriter.newLine();
                }
                if (!visibility.equals("private")) {
                    tempWriter.write("transitive-accessible");
                    tempWriter.write(suffix);
                    tempWriter.newLine();
                }
            } else { // method
                final String methodName = tokens[2].substring(0, methodDescIndex);
                final String methodDesc = tokens[2].substring(methodDescIndex);
                final MappingTree.MethodMapping methodMapping = classMapping.getMethod(methodName, methodDesc, namedNsId);

                if(methodMapping == null) {
                    throw new RuntimeException("Missing method mapping for " + methodName);
                }

                final String mappedName = methodMapping.getName(intermediaryNsId);
				final String mappedDesc = methodMapping.getDesc(intermediaryNsId);

                final String suffix = " method %s %s %s".formatted(className, mappedName, mappedDesc);

                if (unfinal) {
                    tempWriter.write("transitive-extendable");
                    tempWriter.write(suffix);
                    tempWriter.newLine();
                }
                if (visibility.equals("public") || (!unfinal && !visibility.equals("private"))) {
                    tempWriter.write("transitive-accessible");
                    tempWriter.write(suffix);
                    tempWriter.newLine();
                }
            }
        }

        if (widenerReader != null) {
            var foundEnd = false;

            while ((line = widenerReader.readLine()) != null) {
                if (line.matches("^\\s*#\\s*}")) {
                    foundEnd = true;
                    do {
                        tempWriter.write(line);
                        tempWriter.newLine();
                    } while ((line = widenerReader.readLine()) != null);
                    break;
                }
            }

            widenerReader.close();

            if (!foundEnd)
                throw new AssertionError("No \"# }\" block found!");
        } else {
            tempWriter.write("# }");
            tempWriter.newLine();
        }

        tempWriter.flush();
        tempWriter.close();
        transformerReader.close();

        final BufferedReader tempReader = new BufferedReader(new FileReader(tempFile));
        final BufferedWriter widenerWriter = new BufferedWriter(new FileWriter(widenerFile));
        tempReader.transferTo(widenerWriter);
        widenerWriter.flush();
        tempReader.close();
        widenerWriter.close();
    }

    @TaskAction
    public void generateAccessWidenerFromTransformer() throws IOException {
        try (var serviceManager = new ScopedSharedServiceManager()) {
            runGeneration(serviceManager);
        }
    }

}
