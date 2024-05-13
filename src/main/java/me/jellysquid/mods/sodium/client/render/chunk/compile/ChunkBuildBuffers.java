package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.gl.util.VertexRange;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.render.chunk.SharedQuadIndexBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.BakedChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.TranslucentQuadAnalyzer;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A collection of temporary buffers for each worker thread which will be used to build chunk meshes for given render
 * passes. This makes a best-effort attempt to pick a suitable size for each scratch buffer, but will never try to
 * shrink a buffer.
 */
public class ChunkBuildBuffers {
    private static final ModelQuadFacing[] ONLY_UNASSIGNED = new ModelQuadFacing[] { ModelQuadFacing.UNASSIGNED };
    private final Reference2ReferenceOpenHashMap<TerrainRenderPass, BakedChunkModelBuilder> builders = new Reference2ReferenceOpenHashMap<>();

    private final ChunkVertexType vertexType;

    public ChunkBuildBuffers(ChunkVertexType vertexType) {
        this.vertexType = vertexType;

        for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
            var vertexBuffers = new ChunkMeshBufferBuilder[ModelQuadFacing.COUNT];

            if(!pass.isSorted()) {
                // Split vertex data by side
                for (int facing = 0; facing < ModelQuadFacing.COUNT; facing++) {
                    vertexBuffers[facing] = new ChunkMeshBufferBuilder(this.vertexType, 128 * 1024, false);
                }
            } else {
                // We do not split vertex data by side
                vertexBuffers[ModelQuadFacing.UNASSIGNED.ordinal()] = new ChunkMeshBufferBuilder(this.vertexType, 128 * 1024, true);
            }

            this.builders.put(pass, new BakedChunkModelBuilder(vertexBuffers, !pass.isSorted()));
        }
    }

    public void init(BuiltSectionInfo.Builder renderData, int sectionIndex) {
        for (var builder : this.builders.values()) {
            builder.begin(renderData, sectionIndex);
        }
    }

    public ChunkModelBuilder get(Material material) {
        return this.builders.get(material.pass);
    }

    public ChunkModelBuilder get(TerrainRenderPass pass) {
        return this.builders.get(pass);
    }

    /**
     * Creates immutable baked chunk meshes from all non-empty scratch buffers. This is used after all blocks
     * have been rendered to pass the finished meshes over to the graphics card. This function can be called multiple
     * times to return multiple copies.
     */
    public BuiltSectionMeshParts createMesh(TerrainRenderPass pass) {
        var builder = this.builders.get(pass);

        List<ByteBuffer> vertexBuffers = new ArrayList<>();
        VertexRange[] vertexRanges = new VertexRange[ModelQuadFacing.COUNT];

        int vertexCount = 0;

        ModelQuadFacing[] facingsToUpload = pass.isSorted() ? ONLY_UNASSIGNED : ModelQuadFacing.VALUES;
        TranslucentQuadAnalyzer.SortState sortState = pass.isSorted() ? builder.getVertexBuffer(ModelQuadFacing.UNASSIGNED).getSortState() : null;

        for (ModelQuadFacing facing : facingsToUpload) {
            var buffer = builder.getVertexBuffer(facing);

            if (buffer.isEmpty()) {
                continue;
            }

            vertexBuffers.add(buffer.slice());
            vertexRanges[facing.ordinal()] = new VertexRange(vertexCount, buffer.count());

            vertexCount += buffer.count();
        }

        if (vertexCount == 0) {
            return null;
        }

        var mergedBuffer = new NativeBuffer(vertexCount * this.vertexType.getVertexFormat().getStride());
        var mergedBufferBuilder = mergedBuffer.getDirectBuffer();

        for (var buffer : vertexBuffers) {
            mergedBufferBuilder.put(buffer);
        }

        mergedBufferBuilder.flip();

        // Generate the canonical index buffer
        var mergedIndexBuffer = new NativeBuffer((vertexCount / 4 * 6) * 4);
        var mergedIndexBufferBuilder = mergedIndexBuffer.getDirectBuffer();
        for (ModelQuadFacing facing : facingsToUpload) {
            var buffer = builder.getVertexBuffer(facing);

            if (buffer.isEmpty()) {
                continue;
            }

            int numPrimitives = buffer.count() / 4;

            var indexBuffer = ByteBuffer.allocate(numPrimitives * 6 * 4).order(ByteOrder.nativeOrder());

            SharedQuadIndexBuffer.IndexType.INTEGER.createIndexBuffer(indexBuffer, numPrimitives);
            mergedIndexBufferBuilder.put(indexBuffer);
        }

        mergedIndexBufferBuilder.flip();

        return new BuiltSectionMeshParts(mergedBuffer, mergedIndexBuffer, sortState, vertexRanges);
    }

    public void destroy() {
        for (var builder : this.builders.values()) {
            builder.destroy();
        }
    }

    public ChunkVertexType getVertexType() {
        return vertexType;
    }
}
