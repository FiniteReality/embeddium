package me.jellysquid.mods.sodium.mixin;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.Mixin;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class MixinClassValidator {
    private static final String MIXIN_DESC = Type.getDescriptor(Mixin.class);

    public static boolean isMixinClass(Path classPath) {
        byte[] bytecode;

        try {
            bytecode = Files.readAllBytes(classPath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return isMixinClass(fromBytecode(bytecode));
    }

    public static ClassNode fromBytecode(byte[] bytecode) {
        ClassNode node = new ClassNode();
        ClassReader reader = new ClassReader(bytecode);
        reader.accept(node, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);

        return node;
    }

    public static boolean isMixinClass(ClassNode node) {
        if(node.invisibleAnnotations == null) {
            return false;
        }

        return node.invisibleAnnotations.stream().anyMatch(annotation -> annotation.desc.equals(MIXIN_DESC));
    }
}
