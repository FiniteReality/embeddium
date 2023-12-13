package org.embeddedt.embeddium.compat.immersive;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientReloadListenersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLLoader;

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
            MinecraftForge.EVENT_BUS.addListener(ImmersiveConnectionRenderer::meshAppendEvent);
        }
    }
}
