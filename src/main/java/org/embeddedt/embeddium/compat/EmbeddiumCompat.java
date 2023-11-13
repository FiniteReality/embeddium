package org.embeddedt.embeddium.compat;

import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLLoader;
import org.embeddedt.embeddium.compat.immersive.ImmersiveConnectionRenderer;

public class EmbeddiumCompat {
    public static boolean immersiveLoaded = FMLLoader.getLoadingModList().getModFileById("immersiveengineering") != null;

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EmbeddiumCompat::registerReloadListener);

        if(immersiveLoaded) {
            MinecraftForge.EVENT_BUS.addListener(ImmersiveConnectionRenderer::meshAppendEvent);
        }
    }

    public static void registerReloadListener(RegisterClientReloadListenersEvent ev) {
        if(immersiveLoaded)
            ev.registerReloadListener(new ImmersiveConnectionRenderer());
    }
}
