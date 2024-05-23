package org.embeddedt.embeddium.util.task;

public interface CancellationToken {
    boolean isCancelled();

    void setCancelled();
}
