package org.embeddedt.embeddium.gradle;

import org.gradle.api.DefaultTask;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.VerificationTask;
import org.objectweb.asm.*;
import org.objectweb.asm.signature.SignatureReader;
import org.objectweb.asm.signature.SignatureVisitor;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public abstract class VerifyAPICompat extends DefaultTask implements VerificationTask {
    @InputFile
    public abstract RegularFileProperty getBinary();

    private String validateType(Type type) {
        if(type.getSort() == Type.METHOD) {
            for(Type arg : type.getArgumentTypes()) {
                var error = validateType(arg);
                if(error != null) {
                    return error;
                }
            }
            return validateType(type.getReturnType());
        } else if(type.getSort() == Type.ARRAY) {
            return validateType(type.getElementType());
        } else if(type.getSort() == Type.OBJECT) {
            if(type.getClassName().startsWith("org.embeddedt.embeddium.") && !type.getClassName().startsWith("org.embeddedt.embeddium.api.")) {
                return type.getClassName();
            }
        }
        return null;
    }

    @TaskAction
    public void run() {
        try(var is = new ZipInputStream(new FileInputStream(getBinary().get().getAsFile()))) {
            List<String> errors = new ArrayList<>();

            ClassVisitor visitor = new ClassVisitor(Opcodes.ASM9) {
                private String className = "[unknown]";
                private String enclosingSigName, enclosingSigType;

                final SignatureVisitor sigVisitor = new SignatureVisitor(Opcodes.ASM9) {
                    @Override
                    public void visitClassType(String name) {
                        String badType = validateType(Type.getObjectType(name));
                        if(badType != null) {
                            check(badType);
                        }
                        super.visitClassType(name);
                    }
                };

                @Override
                public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
                    className = name.replace("/", ".");
                    super.visit(version, access, name, signature, superName, interfaces);
                }

                private void check(String descriptor) {
                    if(descriptor == null) {
                        return;
                    }
                    errors.add(String.format("API class %s references type %s from %s '%s'", className, descriptor, enclosingSigType, enclosingSigName));
                }

                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if((access & Opcodes.ACC_PRIVATE) == 0) {
                        enclosingSigType = "method";
                        enclosingSigName = name;
                        check(validateType(Type.getMethodType(descriptor)));
                        if(signature != null) {
                            new SignatureReader(signature).accept(sigVisitor);
                        }
                    }
                    return super.visitMethod(access, name, descriptor, signature, exceptions);
                }

                @Override
                public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
                    if((access & Opcodes.ACC_PRIVATE) == 0) {
                        enclosingSigType = "field";
                        enclosingSigName = name;
                        check(validateType(Type.getType(descriptor)));
                        if(signature != null) {
                            new SignatureReader(signature).accept(sigVisitor);
                        }
                    }
                    return super.visitField(access, name, descriptor, signature, value);
                }
            };
            ZipEntry entry;
            while((entry = is.getNextEntry()) != null) {
                if (entry.getName().startsWith("org/embeddedt/embeddium/api") && entry.getName().endsWith(".class")) {
                    ClassReader reader = new ClassReader(is);
                    reader.accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
            }
            if(!errors.isEmpty()) {
                throw new IllegalArgumentException("API violations found:\n" + errors.stream().map(s -> " - " + s + "\n").collect(Collectors.joining()));
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
    }
}