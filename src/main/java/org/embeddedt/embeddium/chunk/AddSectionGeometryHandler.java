package org.embeddedt.embeddium.chunk;

import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.Embeddium;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddSectionGeometryEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

@EventBusSubscriber(modid = Embeddium.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.GAME)
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
