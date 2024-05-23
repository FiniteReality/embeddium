package org.embeddedt.embeddium.util.collections;

import org.jetbrains.annotations.Nullable;

public interface ReadQueue<E> {
    @Nullable E dequeue();
}
