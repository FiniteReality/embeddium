package org.embeddedt.embeddium.compat.immersive;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;

@Mod.EventBusSubscriber(modid = SodiumClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ImmersiveCompat {
    private static final boolean immersiveLoaded = FMLLoader.getLoadingModList().getModFileById("immersiveengineering") != null;
    private static boolean hasRegisteredMeshAppender;

    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        if(!immersiveLoaded)
            return;

        event.registerReloadListener(new ImmersiveConnectionRenderer());
        if(!hasRegisteredMeshAppender) {
            hasRegisteredMeshAppender = true;
            NeoForge.EVENT_BUS.addListener(ImmersiveConnectionRenderer::meshAppendEvent);
        }
    }
}
