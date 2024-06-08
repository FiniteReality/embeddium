package org.embeddedt.phi.asm;

import net.minecraft.launchwrapper.IClassTransformer;

public class PhiPatchingTransformer implements IClassTransformer {
    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        //System.out.println(transformedName);
        return basicClass;
    }
}
