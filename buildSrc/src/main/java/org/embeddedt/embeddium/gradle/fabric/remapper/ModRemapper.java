package org.embeddedt.embeddium.gradle.fabric.remapper;

import com.google.common.io.ByteStreams;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

public class ModRemapper {
    public static void remapMod(InputStream input, OutputStream output, Remapper... mappings) throws IOException {
        JarInputStream jis = new JarInputStream(input);
        Manifest manifest = jis.getManifest();
        if(manifest == null) {
            throw new IOException("Missing manifest for jar");
        }
        JarOutputStream jos = new JarOutputStream(output, manifest);
        JarEntry entry;
        while((entry = jis.getNextJarEntry()) != null) {
            jos.putNextEntry(entry);
            if(entry.getName().endsWith(".class")) {
                // Remap if it is a class file
                byte[] bytecode = ByteStreams.toByteArray(jis);
                try {
                    ClassReader reader = new ClassReader(bytecode);
                    ClassWriter writer = new ClassWriter(0);
                    ClassVisitor visitor = writer;
                    for (int i = mappings.length - 1; i >= 0; i--) {
                        visitor = new ClassRemapper(visitor, mappings[i]);
                    }
                    reader.accept(visitor, 0);
                    bytecode = writer.toByteArray();
                } catch(RuntimeException e) {
                   // RuntimeDeobfLocator.LOGGER.error("Error encountered remapping " + entry.getName(), e);
                }
                jos.write(bytecode);
            } else if(entry.getName().endsWith(".jar") && entry.getName().contains("META-INF/jarjar")) {
                // Probably a nested JAR, remap it
                //RuntimeDeobfLocator.LOGGER.info("Remapping JarJar'ed mod {}", entry.getName());
                ModRemapper.remapMod(jis, jos, mappings);
            } else {
                // Just copy
                ByteStreams.copy(jis, jos);
            }
        }
        jos.finish();
    }
}

