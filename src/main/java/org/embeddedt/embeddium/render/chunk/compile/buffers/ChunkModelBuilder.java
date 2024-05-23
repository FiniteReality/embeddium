package org.embeddedt.embeddium.render.chunk.compile.buffers;

import org.embeddedt.embeddium.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

public interface ChunkModelBuilder {
    ChunkMeshBufferBuilder getVertexBuffer(ModelQuadFacing facing);

    void addSprite(TextureAtlasSprite sprite);

    ChunkModelVertexConsumer asVertexConsumer(Material material);
}
