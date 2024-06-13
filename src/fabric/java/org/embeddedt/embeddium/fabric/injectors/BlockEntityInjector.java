package org.embeddedt.embeddium.fabric.injectors;

import net.minecraft.client.player.LocalPlayer;

public interface BlockEntityInjector {
    default boolean hasCustomOutlineRendering(LocalPlayer player) {
        return false;
    }
}
