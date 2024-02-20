package org.embeddedt.embeddium.client.gui;

import java.util.function.Predicate;
import java.util.function.Supplier;

public record CustomOption<T>(Predicate<T> predicate, Mode mode, Supplier<T> option) {

    public boolean shouldApply(T option) {
        return this.predicate.test(option);
    }

    public T getOption() {
        return this.option.get();
    }

    public enum Mode {
        INJECT_BEFORE, INJECT_AFTER, INJECT_LAST, INJECT_FIRST, REPLACE, DELETE
    }
}
