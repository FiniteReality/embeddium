package org.embeddedt.embeddium.api.eventbus;

import net.minecraftforge.common.MinecraftForge;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Holds a list of event handlers and handles event dispatching.
 */
public class EventHandlerRegistrar<T extends EmbeddiumEvent> {
    private final List<Handler<T>> handlerList = new CopyOnWriteArrayList<>();

    public EventHandlerRegistrar() {}

    public void addListener(Handler<T> listener) {
        handlerList.add(listener);
    }

    /**
     * Post the given event to all registered listeners.
     * @param event The event to post
     * @return true if the event is cancelable and was canceled, false otherwise
     */
    public boolean post(T event) {
        boolean canceled = false;

        // Skip doing work if the handler list is empty
        if(!handlerList.isEmpty()) {
            boolean isCancelable = event.isCancelable();
            for(Handler<T> handler : handlerList) {
                handler.acceptEvent(event);
                if(isCancelable && event.isCanceled()) {
                    canceled = true;
                }
            }
        }

        // Dispatch to the platform event bus as well (currently only used on Forge)
        canceled |= postPlatformSpecificEvent(event);
        return canceled;
    }

    private static <T extends EmbeddiumEvent> boolean postPlatformSpecificEvent(T event) {
        return MinecraftForge.EVENT_BUS.post(event);
    }

    @FunctionalInterface
    public interface Handler<T extends EmbeddiumEvent> {
        void acceptEvent(T event);
    }
}
