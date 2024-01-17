package org.embeddedt.embeddium.chunk;

import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap.Entry;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.BlockAndTintGetter;
import org.embeddedt.embeddium.api.MeshAppender;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import java.util.List;

public class MeshAppenderRenderer {
    private static final Vector3fc ZERO = new Vector3f();

    private static final ThreadLocal<Reference2ReferenceOpenHashMap<RenderType, SinkingVertexBuilder>> BUILDERS = ThreadLocal.withInitial(() -> {
        Reference2ReferenceOpenHashMap<RenderType, SinkingVertexBuilder> map = new Reference2ReferenceOpenHashMap<>();
        for(RenderType layer : RenderType.chunkBufferLayers()) {
            map.put(layer, new SinkingVertexBuilder());
        }
        return map;
    });

    public static void renderMeshAppenders(List<MeshAppender> appenders, BlockAndTintGetter world, SectionPos origin, ChunkBuildBuffers buffers) {
        if (appenders.isEmpty()) {
            return;
        }

        Reference2ReferenceOpenHashMap<RenderType, SinkingVertexBuilder> builders = BUILDERS.get();

        for (var it = builders.reference2ReferenceEntrySet().fastIterator(); it.hasNext(); ) {
            var entry = it.next();

            entry.getValue().reset();
        }

        MeshAppender.Context context = new MeshAppender.Context(builders::get, world, origin, buffers);
        for (MeshAppender appender : appenders) {
            appender.render(context);
        }

        for (var it = builders.reference2ReferenceEntrySet().fastIterator(); it.hasNext(); ) {
            var entry = it.next();
            var material = DefaultMaterials.forRenderLayer(entry.getKey());

            entry.getValue().flush(buffers.get(material), material, ZERO);
        }
    }
}
