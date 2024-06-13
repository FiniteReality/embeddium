package org.embeddedt.embeddium.fabric.init;

import net.fabricmc.api.ClientModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public class EmbeddiumFabricInitializer implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        try {
            Class.forName("org.embeddedt.embeddium.impl.Embeddium").getConstructor(IEventBus.class).newInstance(NeoForge.EVENT_BUS);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
