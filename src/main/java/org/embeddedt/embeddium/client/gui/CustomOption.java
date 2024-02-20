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
        HEAD, TAIL, ADD_BEFORE, ADD_AFTER, REPLACE, DELETE
    }
}
