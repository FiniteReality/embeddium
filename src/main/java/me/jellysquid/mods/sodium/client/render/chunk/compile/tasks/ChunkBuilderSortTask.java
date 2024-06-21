package me.jellysquid.mods.sodium.client.render.chunk.compile.tasks;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import org.embeddedt.embeddium.render.chunk.sorting.TranslucentQuadAnalyzer;

import java.nio.ByteBuffer;
import java.util.Map;

public class ChunkBuilderSortTask extends ChunkBuilderTask<ChunkBuildOutput> {
    private final RenderSection render;
    private final float cameraX, cameraY, cameraZ;
    private final int frame;
    private final Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> translucentMeshes;

    public ChunkBuilderSortTask(RenderSection render, float cameraX, float cameraY, float cameraZ, int frame, Map<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> translucentMeshes) {
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
        for(Map.Entry<TerrainRenderPass, TranslucentQuadAnalyzer.SortState> entry : translucentMeshes.entrySet()) {
            var sortBuffer = entry.getValue();
            var newIndexBuffer = new NativeBuffer(ChunkBufferSorter.getIndexBufferSize(sortBuffer.centers().length / 3));
            ChunkBufferSorter.sort(newIndexBuffer, sortBuffer, cameraX - this.render.getOriginX(), cameraY - this.render.getOriginY(), cameraZ - this.render.getOriginZ());
            meshes.put(entry.getKey(), new BuiltSectionMeshParts(
                    null,
                    newIndexBuffer,
                    sortBuffer,
                    null
            ));
        }
        ChunkBuildOutput result = new ChunkBuildOutput(render, null, meshes, this.frame);
        result.setIndexOnlyUpload(true);
        return result;
    }
}
