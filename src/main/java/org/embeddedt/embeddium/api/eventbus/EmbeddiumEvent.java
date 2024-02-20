package org.embeddedt.embeddium.api.eventbus;

import net.minecraftforge.eventbus.api.Event;

/**
 * The base class which all Embeddium-posted events are derived from.
 * <p></p>
 * On (Neo)Forge, this class will extend their native event class, to allow firing the event to the event bus.
 * <p></p>
 * On Fabric, it extends nothing.
 */
public abstract class EmbeddiumEvent extends Event {
    /**
     * Subclasses must override and return true if they want the event to be canceled.
     */
    @Override
    public boolean isCancelable() {
        return false;
    }

    @Override
    public boolean isCanceled() {
        return super.isCanceled();
    }

    @Override
    public void setCanceled(boolean cancel) {
        super.setCanceled(cancel);
    }
}
