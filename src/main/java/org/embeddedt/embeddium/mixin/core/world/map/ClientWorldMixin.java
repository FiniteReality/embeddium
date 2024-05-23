package org.embeddedt.embeddium.mixin.core.world.map;

import org.embeddedt.embeddium.render.chunk.map.ChunkTracker;
import org.embeddedt.embeddium.render.chunk.map.ChunkTrackerHolder;
import net.minecraft.client.multiplayer.ClientLevel;
import org.apache.commons.lang3.Validate;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(ClientLevel.class)
public class ClientWorldMixin implements ChunkTrackerHolder {
    @Unique
    private final ChunkTracker chunkTracker = new ChunkTracker();

    @Override
    public ChunkTracker sodium$getTracker() {
        return Validate.notNull(this.chunkTracker);
    }
}
