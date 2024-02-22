package org.embeddedt.embeddium.api.eventbus;

/**
 * The base class which all Embeddium-posted events are derived from.
 * <p></p>
 * On (Neo)Forge, this class will extend their native event class, to allow firing the event to the event bus.
 * <p></p>
 * On Fabric, it extends nothing.
 */
public abstract class EmbeddiumEvent {
    private boolean canceled = false;

    /**
     * Subclasses must override and return true if they want the event to be canceled.
     */
    public boolean isCancelable() {
        return false;
    }

    public boolean isCanceled() {
        return canceled;
    }

    public void setCanceled(boolean cancel) {
        canceled = cancel;
    }
}
