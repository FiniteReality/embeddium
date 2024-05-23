package org.embeddedt.embeddium.util.collections;

import org.jetbrains.annotations.NotNull;

public interface WriteQueue<E> {
    void ensureCapacity(int numElements);

    void enqueue(@NotNull E e);
}
