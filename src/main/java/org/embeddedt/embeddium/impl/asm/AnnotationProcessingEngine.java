package org.embeddedt.embeddium.impl.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.service.MixinService;
import org.spongepowered.asm.util.Annotations;

import java.io.IOException;
import java.util.List;

public class AnnotationProcessingEngine {
    private static final String OPTIONAL_INTERFACE_DESC = Type.getDescriptor(OptionalInterface.class);

    public static void processClass(ClassNode clz) {
        for(var annotationNode : clz.invisibleAnnotations) {
            if(annotationNode.desc.equals(OPTIONAL_INTERFACE_DESC)) {
                List<Type> list = Annotations.getValue(annotationNode);
                for(Type ifaceType : list) {
                    String ifaceName = ifaceType.getInternalName();
                    try {
                        MixinService.getService().getBytecodeProvider().getClassNode(ifaceName);
                    } catch(IOException | ClassNotFoundException e) {
                        clz.interfaces.removeIf(i -> i.equals(ifaceName));
                    }
                }
            }
        }
    }
}
