package org.embeddedt.embeddium.api.options.structure;

public interface OptionStorage<T> {
    T getData();

    void save();
}
