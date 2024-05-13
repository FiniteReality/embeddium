package me.jellysquid.mods.sodium.client.render.chunk.compile.buffers;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;

public class BakedChunkModelBuilder implements ChunkModelBuilder {
    private final ChunkMeshBufferBuilder[] vertexBuffers;
    private final boolean splitBySide;

    private BuiltSectionInfo.Builder renderData;

    public BakedChunkModelBuilder(ChunkMeshBufferBuilder[] vertexBuffers, boolean splitBySide) {
        this.vertexBuffers = vertexBuffers;
        this.splitBySide = splitBySide;
    }

    @Override
    public ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing) {
        return splitBySide ? this.vertexBuffers[facing.ordinal()] : this.vertexBuffers[ModelQuadFacing.UNASSIGNED.ordinal()];
    }

    @Override
    public void addSprite(TextureAtlasSprite sprite) {
        this.renderData.addSprite(sprite);
    }

    public void destroy() {
        for (ChunkMeshBufferBuilder builder : this.vertexBuffers) {
            if(builder != null) {
                builder.destroy();
            }
        }
    }

    public void begin(BuiltSectionInfo.Builder renderData, int sectionIndex) {
        this.renderData = renderData;

        for (var vertexBuffer : this.vertexBuffers) {
            if(vertexBuffer != null) {
                vertexBuffer.start(sectionIndex);
            }
        }
    }
}
