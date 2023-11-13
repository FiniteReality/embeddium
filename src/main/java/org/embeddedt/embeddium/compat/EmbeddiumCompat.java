package org.embeddedt.embeddium.compat;

import net.neoforged.fml.javafmlmod.FMLJavaModLoadingContext;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.embeddedt.embeddium.compat.immersive.ImmersiveConnectionRenderer;

public class EmbeddiumCompat {
    public static boolean immersiveLoaded = FMLLoader.getLoadingModList().getModFileById("immersiveengineering") != null;

    public static void init() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(EmbeddiumCompat::registerReloadListener);

        if(immersiveLoaded) {
            NeoForge.EVENT_BUS.addListener(ImmersiveConnectionRenderer::meshAppendEvent);
        }
    }

    public static void registerReloadListener(RegisterClientReloadListenersEvent ev) {
        if(immersiveLoaded)
            ev.registerReloadListener(new ImmersiveConnectionRenderer());
    }
}
