package org.embeddedt.embeddium.impl.bootstrap;

import java.lang.reflect.Method;

/**
 * This is the class you should run to launch Embeddium on top of the Phi platform.
 */
public class EmbeddiumPhiBootstrap {
    public static void main(String[] args) throws Throwable {
        Class<?> phiBootstrap = Class.forName("org.embeddedt.phi.PhiBootstrap");
        Method mainMethod = phiBootstrap.getDeclaredMethod("main", String[].class);
        mainMethod.invoke(null, (Object)args);
    }
}
