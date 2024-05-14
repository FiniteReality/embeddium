package me.jellysquid.mods.sodium.client.render.chunk.region;

import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.arena.PendingUpload;
import me.jellysquid.mods.sodium.client.gl.arena.staging.FallbackStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.MappedStagingBuffer;
import me.jellysquid.mods.sodium.client.gl.arena.staging.StagingBuffer;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class RenderRegionManager {
    private final Long2ReferenceOpenHashMap<RenderRegion> regions = new Long2ReferenceOpenHashMap<>();

    private final StagingBuffer stagingBuffer;

    public RenderRegionManager(CommandList commandList) {
        this.stagingBuffer = createStagingBuffer(commandList);
    }

    public void update() {
        this.stagingBuffer.flip();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            Iterator<RenderRegion> it = this.regions.values()
                    .iterator();

            while (it.hasNext()) {
                RenderRegion region = it.next();
                region.update(commandList);

                if (region.isEmpty()) {
                    region.delete(commandList);

                    it.remove();
                }
            }
        }
    }

    public void uploadMeshes(CommandList commandList, Collection<ChunkBuildOutput> results) {
        for (var entry : this.createMeshUploadQueues(results)) {
            this.uploadMeshes(commandList, entry.getKey(), entry.getValue().stream().filter(o -> !o.isIndexOnlyUpload()).toList());
            this.uploadResorts(commandList, entry.getKey(), entry.getValue().stream().filter(ChunkBuildOutput::isIndexOnlyUpload).toList());
        }
    }

    private void uploadMeshes(CommandList commandList, RenderRegion region, Collection<ChunkBuildOutput> results) {
        var uploads = new ArrayList<PendingSectionUpload>();

        for (ChunkBuildOutput result : results) {
            for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
                var storage = region.getStorage(pass);

                if (storage != null) {
                    storage.removeMeshes(result.render.getSectionIndex());
                }

                BuiltSectionMeshParts mesh = result.getMesh(pass);

                if (mesh != null) {
                    uploads.add(new PendingSectionUpload(result.render, mesh, pass,
                            new PendingUpload(mesh.getVertexData()), mesh.getIndexData() != null ? new PendingUpload(mesh.getIndexData()) : null));
                }
            }
        }

        // If we have nothing to upload, abort!
        if (uploads.isEmpty()) {
            return;
        }

        var resources = region.createResources(commandList);
        var geometryArena = resources.getGeometryArena();

        boolean bufferChanged = geometryArena.upload(commandList, uploads.stream()
                .map(upload -> upload.vertexUpload));

        bufferChanged |= resources.getIndexArena().upload(commandList, uploads.stream()
                .map(upload -> upload.indexUpload).filter(Objects::nonNull));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (bufferChanged) {
            region.refresh(commandList);
        }

        // Collect the upload results
        for (PendingSectionUpload upload : uploads) {
            var storage = region.createStorage(upload.pass);
            storage.setMeshes(upload.section.getSectionIndex(),
                    upload.vertexUpload.getResult(), upload.indexUpload != null ? upload.indexUpload.getResult() : null, upload.meshData.getVertexRanges());
        }
    }

    private void uploadResorts(CommandList commandList, RenderRegion region, Collection<ChunkBuildOutput> results) {
        var uploads = new ArrayList<PendingResortUpload>();

        for (ChunkBuildOutput result : results) {
            for (TerrainRenderPass pass : DefaultTerrainRenderPasses.ALL) {
                BuiltSectionMeshParts mesh = result.getMesh(pass);

                if(mesh == null) {
                    continue;
                }

                var storage = region.getStorage(pass);

                if (storage != null) {
                    storage.removeIndexBuffer(result.render.getSectionIndex());
                }

                Objects.requireNonNull(mesh.getIndexData());

                uploads.add(new PendingResortUpload(result.render, mesh, pass, new PendingUpload(mesh.getIndexData())));
            }
        }

        // If we have nothing to upload, abort!
        if (uploads.isEmpty()) {
            return;
        }

        var resources = region.createResources(commandList);

        boolean bufferChanged = resources.getIndexArena().upload(commandList, uploads.stream()
                .map(upload -> upload.indexUpload).filter(Objects::nonNull));

        // If any of the buffers changed, the tessellation will need to be updated
        // Once invalidated the tessellation will be re-created on the next attempted use
        if (bufferChanged) {
            region.refresh(commandList);
        }

        // Collect the upload results
        for (PendingResortUpload upload : uploads) {
            var storage = region.createStorage(upload.pass);
            storage.replaceIndexBuffer(upload.section.getSectionIndex(), upload.indexUpload.getResult());
        }
    }

    private Reference2ReferenceMap.FastEntrySet<RenderRegion, List<ChunkBuildOutput>> createMeshUploadQueues(Collection<ChunkBuildOutput> results) {
        var map = new Reference2ReferenceOpenHashMap<RenderRegion, List<ChunkBuildOutput>>();

        for (var result : results) {
            var queue = map.computeIfAbsent(result.render.getRegion(), k -> new ArrayList<>());
            queue.add(result);
        }

        return map.reference2ReferenceEntrySet();
    }

    public void delete(CommandList commandList) {
        for (RenderRegion region : this.regions.values()) {
            region.delete(commandList);
        }

        this.regions.clear();
        this.stagingBuffer.delete(commandList);
    }

    public Collection<RenderRegion> getLoadedRegions() {
        return this.regions.values();
    }

    public StagingBuffer getStagingBuffer() {
        return this.stagingBuffer;
    }

    public RenderRegion createForChunk(int chunkX, int chunkY, int chunkZ) {
        return this.create(chunkX >> RenderRegion.REGION_WIDTH_SH,
                chunkY >> RenderRegion.REGION_HEIGHT_SH,
                chunkZ >> RenderRegion.REGION_LENGTH_SH);
    }

    @NotNull
    private RenderRegion create(int x, int y, int z) {
        var key = RenderRegion.key(x, y, z);
        var instance = this.regions.get(key);

        if (instance == null) {
            this.regions.put(key, instance = new RenderRegion(x, y, z, this.stagingBuffer));
        }

        return instance;
    }

    private record PendingSectionUpload(RenderSection section, BuiltSectionMeshParts meshData, TerrainRenderPass pass, PendingUpload vertexUpload, PendingUpload indexUpload) {
        private PendingSectionUpload(RenderSection section, BuiltSectionMeshParts meshData, TerrainRenderPass pass, PendingUpload vertexUpload) {
            this(section, meshData, pass, vertexUpload, null);
        }
    }

    private record PendingResortUpload(RenderSection section, BuiltSectionMeshParts meshData, TerrainRenderPass pass, PendingUpload indexUpload) {

    }


    private static StagingBuffer createStagingBuffer(CommandList commandList) {
        if (SodiumClientMod.options().advanced.useAdvancedStagingBuffers && MappedStagingBuffer.isSupported(RenderDevice.INSTANCE)) {
            return new MappedStagingBuffer(commandList);
        }

        return new FallbackStagingBuffer(commandList);
    }
}
