package org.embeddedt.phi.asm;

import net.minecraft.launchwrapper.IClassNameTransformer;
import net.minecraft.launchwrapper.IClassTransformer;

public class PhiPatchingTransformer implements IClassTransformer, IClassNameTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        System.out.println(transformedName);
        return basicClass;
    }

    @Override
    public String unmapClassName(String name) {
        return name;
    }

    @Override
    public String remapClassName(String name) {
        return name;
    }
}
