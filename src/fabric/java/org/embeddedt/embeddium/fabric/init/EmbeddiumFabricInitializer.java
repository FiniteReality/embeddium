package org.embeddedt.embeddium.fabric.init;

import net.fabricmc.api.ClientModInitializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddiumFabricInitializer implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("EmbeddiumFabricInitializer");

    @Override
    public void onInitializeClient() {
        try {
            Class.forName("org.embeddedt.embeddium.impl.Embeddium").getConstructor(IEventBus.class).newInstance(NeoForge.EVENT_BUS);
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
