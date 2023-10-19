package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.gl.attribute.GlVertexFormat;
import me.jellysquid.mods.sodium.client.gl.buffer.IndexedVertexData;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;

import java.nio.ByteBuffer;
import java.util.EnumMap;
import java.util.Map;

public class ChunkRenderSortTask extends ChunkRenderBuildTask {
    private final RenderSection render;
    private final float cameraX, cameraY, cameraZ;
    private final int frame;
    private final Map<BlockRenderPass, ChunkBufferSorter.SortBuffer> translucentMeshes;

    public ChunkRenderSortTask(RenderSection render, float cameraX, float cameraY, float cameraZ, int frame, Map<BlockRenderPass, ChunkBufferSorter.SortBuffer> translucentMeshes) {
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
    public ChunkBuildResult performBuild(ChunkBuildContext context, CancellationSource cancellationSource) {
        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);
        for(Map.Entry<BlockRenderPass, ChunkBufferSorter.SortBuffer> entry : translucentMeshes.entrySet()) {
            var sortBuffer = entry.getValue();
            ChunkBufferSorter.sort(entry.getValue(), cameraX - this.render.getOriginX(), cameraY - this.render.getOriginY(), cameraZ - this.render.getOriginZ());
            meshes.put(entry.getKey(), new ChunkMeshData(
                    new IndexedVertexData(sortBuffer.vertexFormat(), makeNativeBuffer(sortBuffer.vertexBuffer()), makeNativeBuffer(sortBuffer.indexBuffer())),
                    sortBuffer.parts()
            ));
        }
        ChunkBuildResult result = new ChunkBuildResult(render, null, meshes, this.frame);
        result.setPartialUpload(true);
        return result;
    }

    @Override
    public void releaseResources() {

    }

}
