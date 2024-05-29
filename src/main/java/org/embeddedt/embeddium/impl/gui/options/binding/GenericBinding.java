package org.embeddedt.embeddium.impl.gui.options.binding;

import org.embeddedt.embeddium.api.options.structure.OptionBinding;

import java.util.function.BiConsumer;
import java.util.function.Function;

public class GenericBinding<S, T> implements OptionBinding<S, T> {
    private final BiConsumer<S, T> setter;
    private final Function<S, T> getter;

    public GenericBinding(BiConsumer<S, T> setter, Function<S, T> getter) {
        this.setter = setter;
        this.getter = getter;
    }

    @Override
    public void setValue(S storage, T value) {
        this.setter.accept(storage, value);
    }

    @Override
    public T getValue(S storage) {
        return this.getter.apply(storage);
    }
}