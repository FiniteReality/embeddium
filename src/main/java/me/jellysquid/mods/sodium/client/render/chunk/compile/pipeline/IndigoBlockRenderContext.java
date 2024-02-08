package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.client.renderer.RenderType;
import org.joml.Vector3fc;

import java.util.Map;

/**
 * Adaptation of Indigo's {@link BlockRenderContext} that delegates back to the Sodium renderer.
 */
public class IndigoBlockRenderContext extends BlockRenderContext {
    private final Map<RenderType, SinkingVertexBuilder> vertexBuilderMap = new Object2ObjectOpenHashMap<>();

    @Override
    protected VertexConsumer getVertexConsumer(RenderType layer) {
        return vertexBuilderMap.computeIfAbsent(layer, k -> new SinkingVertexBuilder());
    }

    public void reset() {
        vertexBuilderMap.values().forEach(SinkingVertexBuilder::reset);
    }

    /**
     * Flush the rendered data to Sodium's chunk mesh builder.
     * @param buffers A pack of build buffers for render types
     * @param origin The origin of this block
     */
    public void flush(ChunkBuildBuffers buffers, Vector3fc origin) {
        vertexBuilderMap.forEach((renderType, sinkingVertexBuilder) -> {
            var material = DefaultMaterials.forRenderLayer(renderType);
            var builder = buffers.get(material);
            sinkingVertexBuilder.flush(builder, material, origin);
        });
    }
}
