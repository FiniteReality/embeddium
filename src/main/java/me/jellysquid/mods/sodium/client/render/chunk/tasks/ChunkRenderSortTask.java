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

    public ChunkRenderSortTask(RenderSection render, float cameraX, float cameraY, float cameraZ, int frame) {
        this.render = render;
        this.cameraX = cameraX;
        this.cameraY = cameraY;
        this.cameraZ = cameraZ;
        this.frame = frame;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource) {
        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);
        for(BlockRenderPass pass : BlockRenderPass.VALUES) {
            if(!pass.isTranslucent())
                continue;
            ChunkGraphicsState state = render.getGraphicsState(pass);
            if(state == null)
                continue;
            ChunkMeshData dataToSort = state.getTranslucencyData();
            if(dataToSort == null)
                continue;
            // Work on a copy of that mesh in case a second sort task arrives
            // The original data will be deallocated at upload time
            dataToSort = dataToSort.copy();
            ChunkBufferSorter.sort(dataToSort, cameraX - this.render.getOriginX(), cameraY - this.render.getOriginY(), cameraZ - this.render.getOriginZ());
            meshes.put(pass, dataToSort);
        }
        if(meshes.isEmpty())
            return null;
        ChunkBuildResult result = new ChunkBuildResult(render, null, meshes, this.frame);
        result.setPartialUpload(true);
        return result;
    }

    @Override
    public void releaseResources() {

    }
}
