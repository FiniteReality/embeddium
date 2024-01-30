package org.embeddedt.embeddium.compat.immersive;

import blusunrize.immersiveengineering.api.wires.SectionConnectionRenderer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLLoader;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

@Mod.EventBusSubscriber(modid = SodiumClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ImmersiveCompat {
    private static final boolean immersiveLoaded = FMLLoader.getLoadingModList().getModFileById("immersiveengineering") != null;
    private static boolean hasRegisteredMeshAppender;

    @SubscribeEvent
    public static void onResourceReload(RegisterClientReloadListenersEvent event) {
        if(!immersiveLoaded)
            return;

        if(!hasRegisteredMeshAppender) {
            hasRegisteredMeshAppender = true;
            NeoForge.EVENT_BUS.addListener(ImmersiveCompat::renderIEWires);
        }
    }

    public static void renderIEWires(ChunkMeshEvent event) {
        var renderChecker = SectionConnectionRenderer.SHOULD_RENDER_CONNECTIONS.get();
        if(renderChecker.needsRenderingInSection(event.getSectionOrigin())) {
            event.addMeshAppender(context -> SectionConnectionRenderer.RENDER_CONNECTIONS.get().renderConnectionsInSection(context.vertexConsumerProvider(), context.blockRenderView(), context.sectionOrigin().origin()));
        }
    }
}
