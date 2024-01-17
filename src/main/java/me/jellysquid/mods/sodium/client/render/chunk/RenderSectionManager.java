package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMap;
import it.unimi.dsi.fastutil.longs.Long2ReferenceMaps;
import it.unimi.dsi.fastutil.longs.Long2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import org.embeddedt.embeddium.api.ChunkMeshEvent;
import me.jellysquid.mods.sodium.client.gl.arena.GlBufferArena;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter.SortBuffer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildOutput;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobResult;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobTyped;
import me.jellysquid.mods.sodium.client.render.chunk.compile.executor.ChunkJobCollector;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderMeshingTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderSortTask;
import me.jellysquid.mods.sodium.client.render.chunk.compile.tasks.ChunkBuilderTask;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionInfo;
import me.jellysquid.mods.sodium.client.render.chunk.data.BuiltSectionMeshParts;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.lists.VisibleChunkCollector;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.GraphDirection;
import me.jellysquid.mods.sodium.client.render.chunk.occlusion.OcclusionCuller;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion.DeviceResources;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegionManager;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkMeshFormats;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.render.viewport.CameraTransform;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.MathUtil;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import me.jellysquid.mods.sodium.client.util.task.CancellationToken;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import me.jellysquid.mods.sodium.client.world.cloned.ClonedChunkSectionCache;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.phys.Vec3;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;

public class RenderSectionManager {
    private final ChunkBuilder builder;

    private final Thread renderThread = Thread.currentThread();

    private final RenderRegionManager regions;
    private final ClonedChunkSectionCache sectionCache;

    private final Long2ReferenceMap<RenderSection> sectionByPosition = new Long2ReferenceOpenHashMap<>();

    private final ConcurrentLinkedDeque<ChunkJobResult<ChunkBuildOutput>> buildResults = new ConcurrentLinkedDeque<>();

    private final ChunkRenderer chunkRenderer;

    private final ClientLevel world;

    private final ReferenceSet<RenderSection> sectionsWithGlobalEntities = new ReferenceOpenHashSet<>();

    private final OcclusionCuller occlusionCuller;

    private final int renderDistance;

    private final ChunkVertexType vertexType;

    @NotNull
    private SortedRenderLists renderLists;

    @NotNull
    private Map<ChunkUpdateType, ArrayDeque<RenderSection>> rebuildLists;

    private int lastUpdatedFrame;

    private boolean needsUpdate;

    private @Nullable BlockPos lastCameraPosition;
    private Vec3 cameraPosition = Vec3.ZERO;

    private final boolean translucencySorting;
    private final int translucencyBlockRenderDistance;

    public RenderSectionManager(ClientLevel world, int renderDistance, CommandList commandList) {
        ChunkVertexType vertexType = SodiumClientMod.canUseVanillaVertices() ? ChunkMeshFormats.VANILLA_LIKE : ChunkMeshFormats.COMPACT;

        this.chunkRenderer = new DefaultChunkRenderer(RenderDevice.INSTANCE, vertexType);

        this.vertexType = vertexType;

        this.world = world;
        this.builder = new ChunkBuilder(world, vertexType);

        this.needsUpdate = true;
        this.renderDistance = renderDistance;

        this.regions = new RenderRegionManager(commandList);
        this.sectionCache = new ClonedChunkSectionCache(this.world);

        this.renderLists = SortedRenderLists.empty();
        this.occlusionCuller = new OcclusionCuller(Long2ReferenceMaps.unmodifiable(this.sectionByPosition), this.world);

        this.rebuildLists = new EnumMap<>(ChunkUpdateType.class);

        for (var type : ChunkUpdateType.values()) {
            this.rebuildLists.put(type, new ArrayDeque<>());
        }

        this.translucencySorting = SodiumClientMod.options().performance.useTranslucentFaceSorting;
        this.translucencyBlockRenderDistance = Math.min(9216, (renderDistance << 4) * (renderDistance << 4));
    }

    public void update(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.lastCameraPosition = camera.getBlockPosition();
        this.cameraPosition = camera.getPosition();

        this.scheduleTranslucencyUpdates();

        this.createTerrainRenderList(camera, viewport, frame, spectator);

        this.needsUpdate = false;
        this.lastUpdatedFrame = frame;
    }

    private float lastCameraTranslucentX, lastCameraTranslucentY, lastCameraTranslucentZ;

    private void scheduleTranslucencyUpdates() {
        if(!this.translucencySorting || lastCameraPosition == null)
            return;

        float dx = lastCameraTranslucentX - (float)cameraPosition.x;
        float dy = lastCameraTranslucentY - (float)cameraPosition.y;
        float dz = lastCameraTranslucentZ - (float)cameraPosition.z;
        if((dx * dx + dy * dy + dz * dz) > 1.0) {
            lastCameraTranslucentX = (float)cameraPosition.x;
            lastCameraTranslucentY = (float)cameraPosition.y;
            lastCameraTranslucentZ = (float)cameraPosition.z;
            for (Long2ReferenceMap.Entry<RenderSection> entry : Long2ReferenceMaps.fastIterable(this.sectionByPosition)) {
                var section = entry.getValue();
                if(!section.isBuilt())
                    continue;
                boolean hasTranslucentData = (section.getFlags() & (1 << RenderSectionFlags.HAS_TRANSLUCENT_DATA)) != 0 && section.getTranslucencyData() != null;
                if(hasTranslucentData && section.getSquaredDistance(lastCameraPosition) < translucencyBlockRenderDistance) {
                    ChunkUpdateType update = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(),
                            (allowImportantRebuilds() && this.shouldPrioritizeRebuild(section)) ? ChunkUpdateType.IMPORTANT_SORT : ChunkUpdateType.SORT);
                    if(update != null) {
                        section.setPendingUpdate(update);
                    }
                }
            }
        }
    }

    private void createTerrainRenderList(Camera camera, Viewport viewport, int frame, boolean spectator) {
        this.resetRenderLists();

        final var searchDistance = this.getSearchDistance();
        final var useOcclusionCulling = this.shouldUseOcclusionCulling(camera, spectator);

        var visitor = new VisibleChunkCollector(frame);

        this.occlusionCuller.findVisible(visitor, viewport, searchDistance, useOcclusionCulling, frame);

        this.renderLists = visitor.createRenderLists();
        this.rebuildLists = visitor.getRebuildLists();
    }

    private float getSearchDistance() {
        float distance;

        if (SodiumClientMod.options().performance.useFogOcclusion) {
            distance = this.getEffectiveRenderDistance();
        } else {
            distance = this.getRenderDistance();
        }

        return distance;
    }

    private boolean shouldUseOcclusionCulling(Camera camera, boolean spectator) {
        final boolean useOcclusionCulling;
        BlockPos origin = camera.getBlockPosition();

        if (spectator && this.world.getBlockState(origin)
                .isSolidRender(this.world, origin))
        {
            useOcclusionCulling = false;
        } else {
            useOcclusionCulling = Minecraft.getInstance().smartCull;
        }
        return useOcclusionCulling;
    }

    private void resetRenderLists() {
        this.renderLists = SortedRenderLists.empty();

        for (var list : this.rebuildLists.values()) {
            list.clear();
        }
    }

    public void onSectionAdded(int x, int y, int z) {
        long key = SectionPos.asLong(x, y, z);

        if (this.sectionByPosition.containsKey(key)) {
            return;
        }

        RenderRegion region = this.regions.createForChunk(x, y, z);

        RenderSection renderSection = new RenderSection(region, x, y, z);
        region.addSection(renderSection);

        this.sectionByPosition.put(key, renderSection);

        ChunkAccess chunk = this.world.getChunk(x, z);
        LevelChunkSection section = chunk.getSections()[this.world.getSectionIndexFromSectionY(y)];

        boolean isEmpty = (section == null || section.hasOnlyAir()) && ChunkMeshEvent.post(this.world, SectionPos.of(x, y, z)).isEmpty();
        if (isEmpty) {
            this.updateSectionInfo(renderSection, BuiltSectionInfo.EMPTY);
        } else {
            renderSection.setPendingUpdate(ChunkUpdateType.INITIAL_BUILD);
        }

        this.connectNeighborNodes(renderSection);

        this.needsUpdate = true;
    }

    public void onSectionRemoved(int x, int y, int z) {
        RenderSection section = this.sectionByPosition.remove(SectionPos.asLong(x, y, z));

        if (section == null) {
            return;
        }

        RenderRegion region = section.getRegion();

        if (region != null) {
            region.removeSection(section);
        }

        this.disconnectNeighborNodes(section);
        this.updateSectionInfo(section, null);

        section.delete();

        this.needsUpdate = true;
    }

    public void renderLayer(ChunkRenderMatrices matrices, TerrainRenderPass pass, double x, double y, double z) {
        RenderDevice device = RenderDevice.INSTANCE;
        CommandList commandList = device.createCommandList();

        this.chunkRenderer.render(matrices, commandList, this.renderLists, pass, new CameraTransform(x, y, z));

        commandList.flush();
    }

    public void tickVisibleRenders() {
        Iterator<ChunkRenderList> it = this.renderLists.iterator();

        while (it.hasNext()) {
            ChunkRenderList renderList = it.next();

            var region = renderList.getRegion();
            var iterator = renderList.sectionsWithSpritesIterator();

            if (iterator == null) {
                continue;
            }

            while (iterator.hasNext()) {
                var section = region.getSection(iterator.nextByteAsInt());

                if (section == null) {
                    continue;
                }

                var sprites = section.getAnimatedSprites();

                if (sprites == null) {
                    continue;
                }

                for (TextureAtlasSprite sprite : sprites) {
                    SpriteUtil.markSpriteActive(sprite);
                }
            }
        }
    }

    public boolean isSectionVisible(int x, int y, int z) {
        RenderSection render = this.getRenderSection(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() == this.lastUpdatedFrame;
    }

    public void updateChunks(boolean updateImmediately) {
        this.sectionCache.cleanup();
        this.regions.update();

        var blockingRebuilds = new ChunkJobCollector(Integer.MAX_VALUE, this.buildResults::add);
        var deferredRebuilds = new ChunkJobCollector(this.builder.getSchedulingBudget(), this.buildResults::add);

        this.submitRebuildTasks(blockingRebuilds, ChunkUpdateType.IMPORTANT_REBUILD);
        this.submitRebuildTasks(blockingRebuilds, ChunkUpdateType.IMPORTANT_SORT);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredRebuilds, ChunkUpdateType.REBUILD);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredRebuilds, ChunkUpdateType.INITIAL_BUILD);

        // Always allow at least one sort task to be scheduled
        var deferredSorts = new ChunkJobCollector(Math.max(1, this.builder.getSchedulingBudget()), this.buildResults::add);
        this.submitRebuildTasks(updateImmediately ? blockingRebuilds : deferredSorts, ChunkUpdateType.SORT);

        blockingRebuilds.awaitCompletion(this.builder);
    }

    public void uploadChunks() {
        var results = this.collectChunkBuildResults();

        if (results.isEmpty()) {
            return;
        }

        this.processChunkBuildResults(results);

        for (var result : results) {
            result.delete();
        }

        this.needsUpdate = true;
    }

    private void processChunkBuildResults(ArrayList<ChunkBuildOutput> results) {
        var filtered = filterChunkBuildResults(results);

        this.regions.uploadMeshes(RenderDevice.INSTANCE.createCommandList(), filtered);

        for (var result : filtered) {
            if(result.info != null) {
                this.updateSectionInfo(result.render, result.info);
                if (this.translucencySorting) {
                    // We only change the translucency info on full rebuilds, as sorts can keep using the same data
                    this.updateTranslucencyInfo(result.render, result.meshes.get(DefaultTerrainRenderPasses.TRANSLUCENT));
                }
            }

            var job = result.render.getBuildCancellationToken();

            if (job != null && result.buildTime >= result.render.getLastSubmittedFrame()) {
                result.render.setBuildCancellationToken(null);
            }

            result.render.setLastBuiltFrame(result.buildTime);
        }
    }

    private void updateTranslucencyInfo(RenderSection render, BuiltSectionMeshParts translucencyMesh) {
        if(translucencyMesh == null)
            return;
        var vertexBuffer = translucencyMesh.getVertexData().getDirectBuffer();
        var heapBuffer = ByteBuffer.allocate(vertexBuffer.capacity()).order(ByteOrder.nativeOrder());
        heapBuffer.put(vertexBuffer);
        heapBuffer.flip();
        render.setTranslucencyData(new ChunkBufferSorter.SortBuffer(heapBuffer, this.chunkRenderer.getVertexType(), translucencyMesh.getVertexRanges()));
    }

    private void updateSectionInfo(RenderSection render, BuiltSectionInfo info) {
        render.setInfo(info);

        if (info == null || ArrayUtils.isEmpty(info.globalBlockEntities)) {
            this.sectionsWithGlobalEntities.remove(render);
        } else {
            this.sectionsWithGlobalEntities.add(render);
        }
    }

    private static List<ChunkBuildOutput> filterChunkBuildResults(ArrayList<ChunkBuildOutput> outputs) {
        var map = new Reference2ReferenceLinkedOpenHashMap<RenderSection, ChunkBuildOutput>();

        for (var output : outputs) {
            if (output.render.isDisposed() || output.render.getLastBuiltFrame() > output.buildTime) {
                continue;
            }

            var render = output.render;
            var previous = map.get(render);

            if (previous == null || previous.buildTime < output.buildTime) {
                map.put(render, output);
            }
        }

        return new ArrayList<>(map.values());
    }

    private ArrayList<ChunkBuildOutput> collectChunkBuildResults() {
        ArrayList<ChunkBuildOutput> results = new ArrayList<>();
        ChunkJobResult<ChunkBuildOutput> result;

        while ((result = this.buildResults.poll()) != null) {
            results.add(result.unwrap());
        }

        return results;
    }

    private void submitRebuildTasks(ChunkJobCollector collector, ChunkUpdateType type) {
        var queue = this.rebuildLists.get(type);

        while (!queue.isEmpty() && collector.canOffer()) {
            RenderSection section = queue.remove();

            if (section.isDisposed()) {
                continue;
            }

            // Because Sodium creates the update queue on the frame before it's processed,
            // the update type might no longer match. Filter out such a scenario.
            if (section.getPendingUpdate() != type) {
                continue;
            }

            int frame = this.lastUpdatedFrame;
            ChunkBuilderTask<ChunkBuildOutput> task = type.isSort() ? this.createSortTask(section, frame) : this.createRebuildTask(section, frame);

            if (task == null && type.isSort()) {
                // Ignore sorts that became invalid
                section.setPendingUpdate(null);
                continue;
            }

            if (task != null) {
                var job = this.builder.scheduleTask(task, type.isImportant(), collector::onJobFinished);
                collector.addSubmittedJob(job);

                section.setBuildCancellationToken(job);

                if (!type.isSort()) {
                    // Prevent further sorts from being performed on this section
                    section.setTranslucencyData(null);
                }
            } else {
                var result = ChunkJobResult.successfully(new ChunkBuildOutput(section, BuiltSectionInfo.EMPTY, Collections.emptyMap(), frame));
                this.buildResults.add(result);

                section.setBuildCancellationToken(null);
            }

            section.setLastSubmittedFrame(frame);
            section.setPendingUpdate(null);
        }
    }

    public @Nullable ChunkBuilderMeshingTask createRebuildTask(RenderSection render, int frame) {
        ChunkRenderContext context = WorldSlice.prepare(this.world, render.getPosition(), this.sectionCache);

        if (context == null) {
            return null;
        }

        return new ChunkBuilderMeshingTask(render, context, frame).withCameraPosition(this.cameraPosition);
    }

    public ChunkBuilderSortTask createSortTask(RenderSection render, int frame) {
        Map<TerrainRenderPass, ChunkBufferSorter.SortBuffer> meshes = new Reference2ReferenceOpenHashMap<>();
        var sortBuffer = render.getTranslucencyData();
        if(sortBuffer == null)
            return null;
        meshes.put(DefaultTerrainRenderPasses.TRANSLUCENT, sortBuffer.duplicate());
        return new ChunkBuilderSortTask(render, (float)cameraPosition.x, (float)cameraPosition.y, (float)cameraPosition.z, frame, meshes);
    }

    public void markGraphDirty() {
        this.needsUpdate = true;
    }

    public boolean needsUpdate() {
        return this.needsUpdate;
    }

    public ChunkBuilder getBuilder() {
        return this.builder;
    }

    public void destroy() {
        this.builder.shutdown(); // stop all the workers, and cancel any tasks

        for (var result : this.collectChunkBuildResults()) {
            result.delete(); // delete resources for any pending tasks (including those that were cancelled)
        }

        this.sectionsWithGlobalEntities.clear();
        this.resetRenderLists();

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.regions.delete(commandList);
            this.chunkRenderer.delete(commandList);
        }
    }

    public int getTotalSections() {
        return this.sectionByPosition.size();
    }

    public int getVisibleChunkCount() {
        var sections = 0;
        var iterator = this.renderLists.iterator();

        while (iterator.hasNext()) {
            var renderList = iterator.next();
            sections += renderList.getSectionsWithGeometryCount();
        }

        return sections;
    }

    private void scheduleRebuildOffThread(int x, int y, int z, boolean important) {
        Minecraft.getInstance().submit(() -> this.scheduleRebuild(x, y, z, important));
    }

    public void scheduleRebuild(int x, int y, int z, boolean important) {
        if (Thread.currentThread() != renderThread) {
            this.scheduleRebuildOffThread(x, y, z, important);
            return;
        }

        this.sectionCache.invalidate(x, y, z);

        RenderSection section = this.sectionByPosition.get(SectionPos.asLong(x, y, z));

        if (section != null) {
            ChunkUpdateType pendingUpdate;

            if (allowImportantRebuilds() && (important || this.shouldPrioritizeRebuild(section))) {
                pendingUpdate = ChunkUpdateType.IMPORTANT_REBUILD;
            } else {
                pendingUpdate = ChunkUpdateType.REBUILD;
            }

            pendingUpdate = ChunkUpdateType.getPromotionUpdateType(section.getPendingUpdate(), pendingUpdate);
            if (pendingUpdate != null) {
                section.setPendingUpdate(pendingUpdate);

                this.needsUpdate = true;
            }
        }
    }

    private static final float NEARBY_REBUILD_DISTANCE = Mth.square(16.0f);

    private boolean shouldPrioritizeRebuild(RenderSection section) {
        return this.lastCameraPosition != null && section.getSquaredDistance(this.lastCameraPosition) < NEARBY_REBUILD_DISTANCE;
    }

    private static boolean allowImportantRebuilds() {
        return !SodiumClientMod.options().performance.alwaysDeferChunkUpdates;
    }

    private float getEffectiveRenderDistance() {
        var color = RenderSystem.getShaderFogColor();
        var distance = RenderSystem.getShaderFogEnd();

        var renderDistance = this.getRenderDistance();

        // The fog must be fully opaque in order to skip rendering of chunks behind it
        if (!Mth.equal(color[3], 1.0f)) {
            return renderDistance;
        }

        return Math.min(renderDistance, distance + 0.5f);
    }

    private float getRenderDistance() {
        return this.renderDistance * 16.0f;
    }

    private void connectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = this.getRenderSection(render.getChunkX() + GraphDirection.x(direction),
                    render.getChunkY() + GraphDirection.y(direction),
                    render.getChunkZ() + GraphDirection.z(direction));

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), render);
                render.setAdjacentNode(direction, adj);
            }
        }
    }

    private void disconnectNeighborNodes(RenderSection render) {
        for (int direction = 0; direction < GraphDirection.COUNT; direction++) {
            RenderSection adj = render.getAdjacent(direction);

            if (adj != null) {
                adj.setAdjacentNode(GraphDirection.opposite(direction), null);
                render.setAdjacentNode(direction, null);
            }
        }
    }

    private RenderSection getRenderSection(int x, int y, int z) {
        return this.sectionByPosition.get(SectionPos.asLong(x, y, z));
    }

    public Collection<String> getDebugStrings() {
        List<String> list = new ArrayList<>();

        int count = 0;

        long deviceUsed = 0;
        long deviceAllocated = 0;

        for (var region : this.regions.getLoadedRegions()) {
            var resources = region.getResources();

            if (resources == null) {
                continue;
            }

            var buffer = resources.getGeometryArena();

            deviceUsed += buffer.getDeviceUsedMemory();
            deviceAllocated += buffer.getDeviceAllocatedMemory();

            count++;
        }

        list.add(String.format("Geometry Pool: %d/%d MiB (%d buffers)", MathUtil.toMib(deviceUsed), MathUtil.toMib(deviceAllocated), count));
        list.add(String.format("Transfer Queue: %s", this.regions.getStagingBuffer().toString()));

        list.add(String.format("Chunk Builder: Permits=%02d | Busy=%02d | Total=%02d",
                this.builder.getScheduledJobCount(), this.builder.getBusyThreadCount(), this.builder.getTotalThreadCount())
        );

        list.add(String.format("Chunk Queues: U=%02d (P0=%03d | P1=%03d | P2=%03d)",
                this.buildResults.size(),
                this.rebuildLists.get(ChunkUpdateType.IMPORTANT_REBUILD).size(),
                this.rebuildLists.get(ChunkUpdateType.REBUILD).size(),
                this.rebuildLists.get(ChunkUpdateType.INITIAL_BUILD).size())
        );

        return list;
    }

    public @NotNull SortedRenderLists getRenderLists() {
        return this.renderLists;
    }

    public boolean isSectionBuilt(int x, int y, int z) {
        var section = this.getRenderSection(x, y, z);
        return section != null && section.isBuilt();
    }

    public void onChunkAdded(int x, int z) {
        for (int y = this.world.getMinSection(); y < this.world.getMaxSection(); y++) {
            this.onSectionAdded(x, y, z);
        }
    }

    public void onChunkRemoved(int x, int z) {
        for (int y = this.world.getMinSection(); y < this.world.getMaxSection(); y++) {
            this.onSectionRemoved(x, y, z);
        }
    }

    public Collection<RenderSection> getSectionsWithGlobalEntities() {
        return ReferenceSets.unmodifiable(this.sectionsWithGlobalEntities);
    }

    public ChunkVertexType getVertexType() {
        return this.vertexType;
    }
}
