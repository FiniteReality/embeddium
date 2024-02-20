package org.embeddedt.embeddium.render.world;

import com.google.common.base.Suppliers;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.world.level.BlockAndTintGetter;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.spongepowered.asm.mixin.MixinEnvironment;

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.function.Supplier;

public class WorldSliceLocalGenerator {
    private static final Class<?> WORLD_SLICE_LOCAL_CLASS;
    private static final MethodHandle WORLD_SLICE_LOCAL_CONSTRUCTOR;
    private static final String WORLD_SLICE_LOCAL_CLASS_NAME = "org/embeddedt/embeddium/render/world/WorldSliceLocal";
    private static final String WORLD_SLICE_LOCAL_CLASS_DESC = "L" + WORLD_SLICE_LOCAL_CLASS_NAME + ";";

    // DEFINE_CLASS is borrowed from FerriteCore under MIT as a small utility
    private static final Supplier<Definer> DEFINE_CLASS = Suppliers.memoize(() -> {
        try {
            // Try to create a Java 9+ style class definer
            // These are all public methods, but just don't exist in Java 8
            Method makePrivateLookup = MethodHandles.class.getMethod(
                    "privateLookupIn", Class.class, MethodHandles.Lookup.class
            );
            Object privateLookup = makePrivateLookup.invoke(null, WorldSliceLocalGenerator.class, MethodHandles.lookup());
            Method defineClass = MethodHandles.Lookup.class.getMethod("defineClass", byte[].class);
            return (bytes, name) -> (Class<?>) defineClass.invoke(privateLookup, (Object) bytes);
        } catch (Exception x) {
            try {
                // If that fails, try a Java 8 style definer
                Method defineClass = ClassLoader.class.getDeclaredMethod(
                        "defineClass", String.class, byte[].class, int.class, int.class
                );
                defineClass.setAccessible(true);
                ClassLoader loader = WorldSliceLocalGenerator.class.getClassLoader();
                return (bytes, name) -> (Class<?>) defineClass.invoke(loader, name, bytes, 0, bytes.length);
            } catch (NoSuchMethodException e) {
                // Fail if neither works
                throw new RuntimeException(e);
            }
        }
    });

    static {
        WORLD_SLICE_LOCAL_CLASS = createWrapperClass();
        try {
            WORLD_SLICE_LOCAL_CONSTRUCTOR = MethodHandles.publicLookup()
                    .findConstructor(WORLD_SLICE_LOCAL_CLASS, MethodType.methodType(void.class, WorldSlice.class))
                    .asType(MethodType.methodType(BlockAndTintGetter.class, WorldSlice.class));
        } catch (NoSuchMethodException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Generate a delegate wrapper around {@link me.jellysquid.mods.sodium.client.world.WorldSlice}. This delegate is used
     * to provide a unique BlockAndTintGetter for each subchunk, like vanilla does, while avoiding the huge array allocations
     * associated with WorldSlice.
     *
     * The returned object is guaranteed to implement all interfaces implemented by {@link me.jellysquid.mods.sodium.client.world.WorldSlice}.
     * @param originalSlice the backing world slice for the delegate
     * @return a unique BlockAndTintGetter guaranteed to be reference-unequal with any other one returned by this
     * method, that points to the given WorldSlice
     */
    public static BlockAndTintGetter generate(WorldSlice originalSlice) {
        try {
            return (BlockAndTintGetter)WORLD_SLICE_LOCAL_CONSTRUCTOR.invokeExact(originalSlice);
        } catch(Throwable e) {
            throw new RuntimeException("Exception creating WorldSlice wrapper", e);
        }
    }

    private static final boolean VERIFY = false;

    private static byte[] createWrapperClassBytecode() {
        String worldSliceDesc = Type.getDescriptor(WorldSlice.class);
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = VERIFY ? new CheckClassAdapter(classWriter) : classWriter;

        FieldVisitor fieldVisitor;
        MethodVisitor methodVisitor;

        Class<?>[] interfaces = WorldSlice.class.getInterfaces();

        classVisitor.visit(MixinEnvironment.getCompatibilityLevel().getClassVersion(), Opcodes.ACC_PUBLIC | Opcodes.ACC_SUPER, WORLD_SLICE_LOCAL_CLASS_NAME, null, "java/lang/Object",
                Arrays.stream(interfaces).map(Type::getInternalName).toArray(String[]::new));

        fieldVisitor = classVisitor.visitField(Opcodes.ACC_PRIVATE | Opcodes.ACC_FINAL, "view", worldSliceDesc, null, null);
        fieldVisitor.visitEnd();

        classVisitor.visitSource(null, null);

        // Generate constructor first
        methodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(" + worldSliceDesc + ")V", null, null);
        methodVisitor.visitCode();
        Label label0 = new Label();
        methodVisitor.visitLabel(label0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
        methodVisitor.visitVarInsn(Opcodes.ALOAD, 1);
        methodVisitor.visitFieldInsn(Opcodes.PUTFIELD, WORLD_SLICE_LOCAL_CLASS_NAME, "view", worldSliceDesc);
        methodVisitor.visitInsn(Opcodes.RETURN);
        Label label3 = new Label();
        methodVisitor.visitLabel(label3);
        methodVisitor.visitLocalVariable("this", WORLD_SLICE_LOCAL_CLASS_DESC, null, label0, label3, 0);
        methodVisitor.visitLocalVariable("view", Type.getDescriptor(WorldSlice.class), null, label0, label3, 1);
        methodVisitor.visitMaxs(2, 2);
        methodVisitor.visitEnd();

        // Now generate delegates for each method on WorldSlice's interfaces
        for(Method method : WorldSlice.class.getMethods()) {
            // Only delegate for public, non-static methods implemented by WorldSlice or an interface
            if(Modifier.isPublic(method.getModifiers()) && !Modifier.isStatic(method.getModifiers()) && !method.getDeclaringClass().isAssignableFrom(Object.class)) {
                int maxStack = 0;
                String methodDescription = Type.getMethodDescriptor(method);
                methodVisitor = classVisitor.visitMethod(Opcodes.ACC_PUBLIC, method.getName(), methodDescription, null, null);
                methodVisitor.visitCode();
                // push WorldSlice
                methodVisitor.visitVarInsn(Opcodes.ALOAD, 0);
                methodVisitor.visitFieldInsn(Opcodes.GETFIELD, WORLD_SLICE_LOCAL_CLASS_NAME, "view", worldSliceDesc);
                maxStack++;
                // push each argument
                int maxLocals = 1;
                for (Type t : Type.getArgumentTypes(method)) {
                    int size = t.getSize();
                    methodVisitor.visitVarInsn(t.getOpcode(Opcodes.ILOAD), maxLocals);
                    maxLocals += size;
                    maxStack += size;
                }
                // invoke the method on WorldSlice
                boolean itf = method.getDeclaringClass().isInterface();
                methodVisitor.visitMethodInsn(itf ? Opcodes.INVOKEINTERFACE : Opcodes.INVOKEVIRTUAL, Type.getInternalName(method.getDeclaringClass()), method.getName(), methodDescription, itf);
                Type returnType = Type.getReturnType(methodDescription);
                methodVisitor.visitInsn(returnType.getOpcode(Opcodes.IRETURN));
                methodVisitor.visitMaxs(maxStack, maxLocals);
                methodVisitor.visitEnd();
            }
        }

        classVisitor.visitEnd();

        return classWriter.toByteArray();
    }

    private static Class<?> createWrapperClass() {
        byte[] bytes = createWrapperClassBytecode();
        try {
            return DEFINE_CLASS.get().define(bytes, WORLD_SLICE_LOCAL_CLASS_NAME.replace('/', '.'));
        } catch(Exception e) {
            throw new RuntimeException("Error defining WorldSlice wrapper", e);
        }
    }

    public static void testClassGeneration() {
        try {
            Files.write(new File("/tmp/WorldSliceLocal.class").toPath(), createWrapperClassBytecode(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch(IOException e) {
            e.printStackTrace();
        }
    }

    private interface Definer {
        Class<?> define(byte[] bytes, String name) throws Exception;
    }
}
