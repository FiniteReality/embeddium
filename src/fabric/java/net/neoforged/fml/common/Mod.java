package net.neoforged.fml.common;

import net.neoforged.api.distmarker.Dist;

/**
 * Phi will bootstrap this class.
 */
public @interface Mod {
    String value();

    Dist[] dist() default { Dist.CLIENT, Dist.DEDICATED_SERVER };
}
