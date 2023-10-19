package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

import java.util.EnumMap;
import java.util.Map;

public class ChunkRenderSortTask extends ChunkRenderBuildTask {
    private final RenderSection render;
    private final float cameraX, cameraY, cameraZ;
    private final int frame;
    private final Map<BlockRenderPass, ChunkMeshData> translucentMeshes;

    public ChunkRenderSortTask(RenderSection render, float cameraX, float cameraY, float cameraZ, int frame, Map<BlockRenderPass, ChunkMeshData> translucentMeshes) {
        this.render = render;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.frame = frame;
        this.translucentMeshes = translucentMeshes;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource) {
        for(Map.Entry<BlockRenderPass, ChunkMeshData> entry : translucentMeshes.entrySet()) {
            ChunkBufferSorter.sort(entry.getValue(), cameraX - this.render.getOriginX(), cameraY - this.render.getOriginY(), cameraZ - this.render.getOriginZ());
        }
        ChunkBuildResult result = new ChunkBuildResult(render, null, translucentMeshes, this.frame);
        result.setPartialUpload(true);
        return result;
    }

    @Override
    public void releaseResources() {

    }
}
