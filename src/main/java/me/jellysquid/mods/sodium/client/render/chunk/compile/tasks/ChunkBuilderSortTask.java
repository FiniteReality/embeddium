package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

public class ChunkBuilderSortTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final float cameraX, cameraY, cameraZ;
    private final int frame;
    private final Map<TerrainRenderPass, ChunkBufferSorter.SortBuffer> translucentMeshes;

    public ChunkBuilderSortTask(RenderSection render, float cameraX, float cameraY, float cameraZ, int frame, Map<TerrainRenderPass, ChunkBufferSorter.SortBuffer> translucentMeshes) {
        this.render = render;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.frame = frame;
        this.translucentMeshes = translucentMeshes;
    }

    private static NativeBuffer makeNativeBuffer(ByteBuffer heapBuffer) {
        heapBuffer.rewind();
        NativeBuffer nb = new NativeBuffer(heapBuffer.capacity());
        nb.getDirectBuffer().put(heapBuffer);
        return nb;
    }

    @Override
    public ChunkBuildOutput execute(ChunkBuildContext context, CancellationToken cancellationSource) {
        Map<TerrainRenderPass, BuiltSectionMeshParts> meshes = new Reference2ReferenceOpenHashMap<>();
        for(Map.Entry<TerrainRenderPass, ChunkBufferSorter.SortBuffer> entry : translucentMeshes.entrySet()) {
            var sortBuffer = entry.getValue();
            ChunkBufferSorter.sort(entry.getValue(), cameraX - this.render.getOriginX(), cameraY - this.render.getOriginY(), cameraZ - this.render.getOriginZ());
            meshes.put(entry.getKey(), new BuiltSectionMeshParts(
                    makeNativeBuffer(sortBuffer.vertexBuffer()),
                    sortBuffer.ranges()
            ));
        }
        ChunkBuildOutput result = new ChunkBuildOutput(render, null, meshes, this.frame);
        result.setPartialUpload(true);
        return result;
    }
}
