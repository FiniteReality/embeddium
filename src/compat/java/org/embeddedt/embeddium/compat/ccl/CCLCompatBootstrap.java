package org.embeddedt.embeddium.compat.ccl;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.embeddedt.embeddium.impl.Embeddium;

@EventBusSubscriber(modid = Embeddium.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class CCLCompatBootstrap {
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        if (ModList.get().isLoaded("codechickenlib")) {
            CCLCompat.onClientSetup(event);
        }
    }
}
