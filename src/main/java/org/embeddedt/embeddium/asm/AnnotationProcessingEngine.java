package org.embeddedt.embeddium.asm;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.util.Annotations;

public class AnnotationProcessingEngine {
    private static final String OPTIONAL_INTERFACE_DESC = Type.getDescriptor(OptionalInterface.class);

    public static void processClass(ClassNode clz) {
        for(var annotationNode : clz.invisibleAnnotations) {
            if(annotationNode.desc.equals(OPTIONAL_INTERFACE_DESC)) {
                Type ifaceType = Annotations.getValue(annotationNode);
                String ifaceName = ifaceType.getInternalName();
                clz.interfaces.removeIf(i -> i.equals(ifaceName));
            }
        }
    }
}
