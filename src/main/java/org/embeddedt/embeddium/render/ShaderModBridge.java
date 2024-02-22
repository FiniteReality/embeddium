package org.embeddedt.embeddium.render;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;

public class ShaderModBridge {
    private static final MethodHandle SHADERS_ENABLED;

    static {
        MethodHandle shadersEnabled = null;
        try {
            Class<?> irisApiClass = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Method instanceGetter = irisApiClass.getDeclaredMethod("getInstance");
            Object irisApiInstance = instanceGetter.invoke(null);
            shadersEnabled = MethodHandles.lookup().unreflect(irisApiClass.getDeclaredMethod("isShaderPackInUse")).bindTo(irisApiInstance);
        } catch (Throwable ignored) {
        }
        SHADERS_ENABLED = shadersEnabled;
    }

    public static boolean areShadersEnabled() {
        if(SHADERS_ENABLED != null) {
            try {
                return (boolean)SHADERS_ENABLED.invokeExact();
            } catch (Throwable e) {
                return false;
            }
        } else {
            return false;
        }
    }
}
