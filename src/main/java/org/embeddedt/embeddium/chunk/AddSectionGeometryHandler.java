package org.embeddedt.embeddium.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

@Mod.EventBusSubscriber(modid = SodiumClientMod.MODID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class AddSectionGeometryHandler {
    private static final ThreadLocal<PoseStack> DUMMY_POSE_STACK = ThreadLocal.withInitial(PoseStack::new);

    @SubscribeEvent
    public static void onChunkMesh(ChunkMeshEvent meshEvent) {
        AddSectionGeometryEvent geometryEvent = new AddSectionGeometryEvent(meshEvent.getSectionOrigin().origin(), meshEvent.getWorld());
        NeoForge.EVENT_BUS.post(geometryEvent);
        if(!geometryEvent.getAdditionalRenderers().isEmpty()) {
            for (var renderer : geometryEvent.getAdditionalRenderers()) {
                meshEvent.addMeshAppender(ctx -> {
                    renderer.render(new AddSectionGeometryEvent.SectionRenderingContext(ctx.vertexConsumerProvider(), ctx.blockRenderView(), DUMMY_POSE_STACK.get()));
                });
            }
        }
    }
}
