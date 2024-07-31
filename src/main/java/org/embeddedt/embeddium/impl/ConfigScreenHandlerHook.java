package org.embeddedt.embeddium.impl;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLConstructModEvent;
import org.embeddedt.embeddium.api.EmbeddiumConstants;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;

@Mod.EventBusSubscriber(modid = EmbeddiumConstants.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ConfigScreenHandlerHook {
    @SubscribeEvent
    public static void onModConstruct(FMLConstructModEvent event) {
        ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class, () -> new ConfigScreenHandler.ConfigScreenFactory((mc, screen) -> new EmbeddiumVideoOptionsScreen(screen)));
    }
}
