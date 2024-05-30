package org.embeddedt.embeddium.impl.asm;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Removes the specified interface from the target class if it doesn't exist.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
public @interface OptionalInterface {
    Class<?>[] value();
}
