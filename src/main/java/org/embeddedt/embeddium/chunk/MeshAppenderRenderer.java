package org.embeddedt.embeddium.chunk;

import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import org.embeddedt.embeddium.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.render.chunk.terrain.material.DefaultMaterials;
import org.embeddedt.embeddium.render.chunk.terrain.material.Material;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.embeddedt.embeddium.api.MeshAppender;

import java.util.List;

public class MeshAppenderRenderer {
    public static void renderMeshAppenders(List<MeshAppender> appenders, BlockAndTintGetter world, SectionPos origin, ChunkBuildBuffers buffers) {
        if (appenders.isEmpty()) {
            return;
        }

        ReferenceArraySet<Material> usedMaterials = new ReferenceArraySet<>();

        MeshAppender.Context context = new MeshAppender.Context(type -> {
            var material = DefaultMaterials.forRenderLayer(type);
            usedMaterials.add(material);
            return buffers.get(material).asVertexConsumer(material);
        }, world, origin, buffers);

        for (MeshAppender appender : appenders) {
            appender.render(context);
        }

        if (usedMaterials.isEmpty()) {
            return;
        }

        for(Material material : usedMaterials) {
            buffers.get(material).asVertexConsumer(material).close();
        }
    }
}
