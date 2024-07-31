package org.embeddedt.embeddium.api.render.clouds;

import net.minecraft.client.Options;
import org.embeddedt.embeddium.api.eventbus.EmbeddiumEvent;
import org.embeddedt.embeddium.api.eventbus.EventHandlerRegistrar;
import org.jetbrains.annotations.ApiStatus;

/**
 * Fired to allow some control over the portions of the optimized cloud renderer Embeddium uses that do not
 * delegate to vanilla logic. Other properties like cloud color may be controlled with a mixin targeting the appropriate
 * vanilla function, or an existing modloader API.
 */
public class ModifyCloudRenderingEvent extends EmbeddiumEvent {
    public static final EventHandlerRegistrar<ModifyCloudRenderingEvent> BUS = new EventHandlerRegistrar<>();

    private int cloudRenderDistance;

    @ApiStatus.Internal
    public ModifyCloudRenderingEvent(int cloudRenderDistance) {
        this.cloudRenderDistance = cloudRenderDistance;
    }

    public int getCloudRenderDistance() {
        return this.cloudRenderDistance;
    }

    /**
     * Set the render distance that will be used for cloud rendering. The default is to use {@link Options#getEffectiveRenderDistance()}.
     * @param distance new render distance
     */
    public void setCloudRenderDistance(int distance) {
        if (distance < 1) {
            throw new IllegalArgumentException("Distance must be positive");
        }
        this.cloudRenderDistance = distance;
    }
}