package me.jellysquid.mods.sodium.client.gl.compile;

import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;

public class ChunkBuildContext {
    public final ChunkBuildBuffers buffers;
    public final ChunkRenderCacheLocal cache;

    public ChunkBuildContext(Level world, ChunkVertexType vertexType, BlockRenderPassManager renderPassManager) {
        this.buffers = new ChunkBuildBuffers(vertexType, renderPassManager);
        this.cache = new ChunkRenderCacheLocal(Minecraft.getInstance(), world);
    }

    public void release() {
        this.buffers.destroy();
    }
}
