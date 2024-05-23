package org.embeddedt.embeddium.util.iterator;

public interface ByteIterator {
    boolean hasNext();

    int nextByteAsInt();
}
