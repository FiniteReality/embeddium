package me.jellysquid.mods.sodium.common.util.collections;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.ReportedException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.BooleanSupplier;

public class WorkStealingFutureDrain<T> implements Iterator<T> {
    private final BooleanSupplier workStealFunction;
    private final LinkedList<CompletableFuture<T>> queue;
    private T next = null;

    public WorkStealingFutureDrain(LinkedList<CompletableFuture<T>> queue, BooleanSupplier workStealFunction) {
        this.queue = queue;
        this.workStealFunction = workStealFunction;
    }

    @Override
    public boolean hasNext() {
        if (this.next != null) {
            return true;
        }

        return (this.next = this.findNext()) != null;
    }

    private T findNext() {
        boolean shouldStealWork = true;

        while (!this.queue.isEmpty()) {
            var iterator = this.queue.iterator();

            while (iterator.hasNext()) {
                var future = iterator.next();

                if (shouldStealWork && !future.isDone()) {
                    continue;
                }

                iterator.remove();

                try {
                    var result = future.join();
                    if (result != null) // null means cancelled
                        return result;
                } catch (CancellationException e) {
                    SodiumClientMod.logger()
                            .warn("Future was cancelled: {}", future);
                } catch (CompletionException e) {
                    if (e.getCause() instanceof ReportedException) {
                        // Propagate CrashExceptions directly to provide extra information
                        throw (ReportedException) e.getCause();
                    } else {
                        throw e;
                    }
                }
            }

            if (shouldStealWork && !this.workStealFunction.getAsBoolean()) {
                shouldStealWork = false;
            }
        }

        return null;
    }

    @Override
    public T next() {
        if (!this.hasNext()) {
            throw new NoSuchElementException();
        }

        T result = this.next;
        this.next = null;

        return result;
    }
}
