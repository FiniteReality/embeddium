package me.jellysquid.mods.sodium.client.render;

import com.google.common.collect.Iterators;
import com.mojang.blaze3d.platform.Window;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.model.quad.blender.BlendedColorProvider;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.lists.ChunkRenderList;
import me.jellysquid.mods.sodium.client.render.chunk.lists.SortedRenderLists;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkStatus;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTracker;
import me.jellysquid.mods.sodium.client.render.chunk.map.ChunkTrackerHolder;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.DefaultTerrainRenderPasses;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.TerrainRenderPass;
import me.jellysquid.mods.sodium.client.render.viewport.Viewport;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.iterator.ByteIterator;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderBuffers;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * Provides an extension to vanilla's {@link LevelRenderer}.
 */
public class SodiumWorldRenderer {
    private static final boolean ENABLE_BLOCKENTITY_CULLING = FMLLoader.getLoadingModList().getModFileById("valkyrienskies") == null;

    private final Minecraft client;

    private ClientLevel world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    private Viewport currentViewport;

    private RenderSectionManager renderSectionManager;

    /**
     * @return The SodiumWorldRenderer based on the current dimension
     */
    public static SodiumWorldRenderer instance() {
        var instance = instanceNullable();

        if (instance == null) {
            throw new IllegalStateException("No renderer attached to active world");
        }

        return instance;
    }

    /**
     * @return The SodiumWorldRenderer based on the current dimension, or null if none is attached
     */
    public static SodiumWorldRenderer instanceNullable() {
        var world = Minecraft.getInstance().levelRenderer;

        if (world instanceof WorldRendererExtended) {
            return ((WorldRendererExtended) world).sodium$getWorldRenderer();
        }

        return null;
    }

    public SodiumWorldRenderer(Minecraft client) {
        this.client = client;
    }

    public void setWorld(ClientLevel world) {
        // Check that the world is actually changing
        if (this.world == world) {
            return;
        }

        // If we have a world is already loaded, unload the renderer
        if (this.world != null) {
            this.unloadWorld();
        }

        // If we're loading a new world, load the renderer
        if (world != null) {
            this.loadWorld(world);
        }
    }

    private void loadWorld(ClientLevel world) {
        this.world = world;

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
        }
    }

    private void unloadWorld() {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.world = null;
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.renderSectionManager.getVisibleChunkCount();
    }

    /**
     * Notifies the chunk renderer that the graph scene has changed and should be re-computed.
     */
    public void scheduleTerrainUpdate() {
        // BUG: seems to be called before init
        if (this.renderSectionManager != null) {
            this.renderSectionManager.markGraphDirty();
        }
    }

    /**
     * @return True if no chunks are pending rebuilds
     */
    public boolean isTerrainRenderComplete() {
        return this.renderSectionManager.getBuilder().isBuildQueueEmpty();
    }

    /**
     * Called prior to any chunk rendering in order to update necessary state.
     */
    public void setupTerrain(Camera camera,
                             Viewport viewport,
                             @Deprecated(forRemoval = true) int frame,
                             boolean spectator,
                             boolean updateChunksImmediately) {
        NativeBuffer.reclaim(false);

        this.processChunkEvents();

        this.useEntityCulling = SodiumClientMod.options().performance.useEntityCulling;

        if (this.client.options.getEffectiveRenderDistance() != this.renderDistance) {
            this.reload();
        }

        ProfilerFiller profiler = this.client.getProfiler();
        profiler.push("camera_setup");

        LocalPlayer player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        Vec3 pos = camera.getPosition();
        float pitch = camera.getXRot();
        float yaw = camera.getYRot();
        float fogDistance = RenderSystem.getShaderFogEnd();

        boolean dirty = pos.x != this.lastCameraX || pos.y != this.lastCameraY || pos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw || fogDistance != this.lastFogDistance;

        if (dirty) {
            this.renderSectionManager.markGraphDirty();
        }

        this.currentViewport = viewport;

        this.lastCameraX = pos.x;
        this.lastCameraY = pos.y;
        this.lastCameraZ = pos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;
        this.lastFogDistance = fogDistance;

        this.renderSectionManager.runAsyncTasks();

        profiler.popPush("chunk_update");

        this.renderSectionManager.updateChunks(updateChunksImmediately);

        profiler.popPush("chunk_upload");

        this.renderSectionManager.uploadChunks();

        if (this.renderSectionManager.needsUpdate()) {
            profiler.popPush("chunk_render_lists");

            this.renderSectionManager.update(camera, viewport, frame, spectator);
        }

        if (updateChunksImmediately) {
            profiler.popPush("chunk_upload_immediately");

            this.renderSectionManager.uploadChunks();
        }

        profiler.popPush("chunk_render_tick");

        this.renderSectionManager.tickVisibleRenders();

        profiler.pop();

        Entity.setViewScale(Mth.clamp((double) this.client.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * this.client.options.entityDistanceScaling().get());
    }

    private void processChunkEvents() {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.forEachEvent(this.renderSectionManager::onChunkAdded, this.renderSectionManager::onChunkRemoved);
    }

    /**
     * Performs a render pass for the given {@link RenderType} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderType renderLayer, PoseStack matrixStack, double x, double y, double z) {
        ChunkRenderMatrices matrices = ChunkRenderMatrices.from(matrixStack);

        List<TerrainRenderPass> passes = DefaultTerrainRenderPasses.RENDER_PASS_MAPPINGS.get(renderLayer);

        if (passes != null) {
            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < passes.size(); i++) {
                this.renderSectionManager.renderLayer(matrices, passes.get(i), x, y, z);
            }
        }
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
        }
    }

    private void initRenderer(CommandList commandList) {
        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.renderDistance = this.client.options.getEffectiveRenderDistance();

        this.renderSectionManager = new RenderSectionManager(this.world, this.renderDistance, commandList);

        var tracker = ChunkTrackerHolder.get(this.world);
        ChunkTracker.forEachChunk(tracker.getReadyChunks(), this.renderSectionManager::onChunkAdded);

        // Forge workaround - reset VSync flag
        var window = Minecraft.getInstance().getWindow();
        if(window != null)
            window.updateVsync(Minecraft.getInstance().options.enableVsync().get());

        BlendedColorProvider.checkBlendingEnabled();
    }

    // We track whether a block entity uses custom block outline rendering, so that the outline postprocessing
    // shader will be enabled appropriately
    private boolean blockEntityRequestedOutline;

    public boolean didBlockEntityRequestOutline() {
        return blockEntityRequestedOutline;
    }

    /**
     * {@return an iterator over all visible block entities}
     * <p>
     * Note that this method performs significantly more allocations and will generally be less efficient than
     * {@link SodiumWorldRenderer#forEachVisibleBlockEntity(Consumer)}. It is intended only for situations where using
     * that method is not feasible.
     */
    public Iterator<BlockEntity> blockEntityIterator() {
        List<Iterator<BlockEntity>> iterators = new ArrayList<>();

        SortedRenderLists renderLists = this.renderSectionManager.getRenderLists();
        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                var blockEntities = renderSection.getCulledBlockEntities();

                if (blockEntities == null) {
                    continue;
                }

                iterators.add(Iterators.forArray(blockEntities));
            }
        }

        for (var renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
            var blockEntities = renderSection.getGlobalBlockEntities();

            if (blockEntities == null) {
                continue;
            }

            iterators.add(Iterators.forArray(blockEntities));
        }

        if(iterators.isEmpty()) {
            return Collections.emptyIterator();
        } else {
            return Iterators.concat(iterators.iterator());
        }
    }

    public void forEachVisibleBlockEntity(Consumer<BlockEntity> consumer) {
        SortedRenderLists renderLists = this.renderSectionManager.getRenderLists();
        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                var blockEntities = renderSection.getCulledBlockEntities();

                if (blockEntities == null) {
                    continue;
                }

                for (BlockEntity blockEntity : blockEntities) {
                    consumer.accept(blockEntity);
                }
            }
        }

        for (var renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
            var blockEntities = renderSection.getGlobalBlockEntities();

            if (blockEntities == null) {
                continue;
            }

            for (var blockEntity : blockEntities) {
                consumer.accept(blockEntity);
            }
        }
    }

    public void renderBlockEntities(PoseStack matrices,
                                    RenderBuffers bufferBuilders,
                                    Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                    Camera camera,
                                    float tickDelta) {
        MultiBufferSource.BufferSource immediate = bufferBuilders.bufferSource();

        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x();
        double y = cameraPos.y();
        double z = cameraPos.z();

        BlockEntityRenderDispatcher blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        this.blockEntityRequestedOutline = false;

        this.renderBlockEntities(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer);
        this.renderGlobalBlockEntities(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer);
    }

    private void renderBlockEntities(PoseStack matrices,
                                     RenderBuffers bufferBuilders,
                                     Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                     float tickDelta,
                                     MultiBufferSource.BufferSource immediate,
                                     double x,
                                     double y,
                                     double z,
                                     BlockEntityRenderDispatcher blockEntityRenderer) {
        SortedRenderLists renderLists = this.renderSectionManager.getRenderLists();
        Iterator<ChunkRenderList> renderListIterator = renderLists.iterator();

        while (renderListIterator.hasNext()) {
            var renderList = renderListIterator.next();

            var renderRegion = renderList.getRegion();
            var renderSectionIterator = renderList.sectionsWithEntitiesIterator();

            if (renderSectionIterator == null) {
                continue;
            }

            while (renderSectionIterator.hasNext()) {
                var renderSectionId = renderSectionIterator.nextByteAsInt();
                var renderSection = renderRegion.getSection(renderSectionId);

                var blockEntities = renderSection.getCulledBlockEntities();

                if (blockEntities == null) {
                    continue;
                }

                for (BlockEntity blockEntity : blockEntities) {
                    if(ENABLE_BLOCKENTITY_CULLING && !currentViewport.isBoxVisible(blockEntity.getRenderBoundingBox()))
                        continue;

                    if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                        this.blockEntityRequestedOutline = true;
                    }

                    renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
                }
            }
        }
    }

    private void renderGlobalBlockEntities(PoseStack matrices,
                                           RenderBuffers bufferBuilders,
                                           Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                           float tickDelta,
                                           MultiBufferSource.BufferSource immediate,
                                           double x,
                                           double y,
                                           double z,
                                           BlockEntityRenderDispatcher blockEntityRenderer) {
        for (var renderSection : this.renderSectionManager.getSectionsWithGlobalEntities()) {
            var blockEntities = renderSection.getGlobalBlockEntities();

            if (blockEntities == null) {
                continue;
            }

            for (var blockEntity : blockEntities) {
                if(ENABLE_BLOCKENTITY_CULLING && !currentViewport.isBoxVisible(blockEntity.getRenderBoundingBox()))
                    continue;

                if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                    this.blockEntityRequestedOutline = true;
                }

                renderBlockEntity(matrices, bufferBuilders, blockBreakingProgressions, tickDelta, immediate, x, y, z, blockEntityRenderer, blockEntity);
            }
        }
    }

    private static void renderBlockEntity(PoseStack matrices,
                                          RenderBuffers bufferBuilders,
                                          Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                          float tickDelta,
                                          MultiBufferSource.BufferSource immediate,
                                          double x,
                                          double y,
                                          double z,
                                          BlockEntityRenderDispatcher dispatcher,
                                          BlockEntity entity) {
        BlockPos pos = entity.getBlockPos();

        matrices.pushPose();
        matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

        MultiBufferSource consumer = immediate;
        SortedSet<BlockDestructionProgress> breakingInfo = blockBreakingProgressions.get(pos.asLong());

        if (breakingInfo != null && !breakingInfo.isEmpty()) {
            int stage = breakingInfo.last().getProgress();

            if (stage >= 0) {
                var bufferBuilder = bufferBuilders.crumblingBufferSource()
                        .getBuffer(ModelBakery.DESTROY_TYPES.get(stage));

                PoseStack.Pose entry = matrices.last();
                VertexConsumer transformer = new SheetedDecalTextureGenerator(bufferBuilder,
                        entry.pose(), entry.normal());

                consumer = (layer) -> layer.affectsCrumbling() ? VertexMultiConsumer.create(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
            }
        }

        try {
            dispatcher.render(entity, tickDelta, matrices, consumer);
        } catch(RuntimeException e) {
            // We catch errors from removed block entities here, because we often end up being faster
            // than vanilla, and rendering them when they wouldn't be rendered by vanilla, which can
            // cause crashes. However, we do not apply this suppression to regular rendering.
            if (!entity.isRemoved()) {
                throw e;
            } else {
                SodiumClientMod.logger().error("Suppressing crash from removed block entity", e);
            }
        }

        matrices.popPose();
    }



    // the volume of a section multiplied by the number of sections to be checked at most
    private static final double MAX_ENTITY_CHECK_VOLUME = 16 * 16 * 16 * 15;

    private static boolean isInfiniteExtentsBox(AABB box) {
        return Double.isInfinite(box.minX) || Double.isInfinite(box.minY) || Double.isInfinite(box.minZ)
            || Double.isInfinite(box.maxX) || Double.isInfinite(box.maxY) || Double.isInfinite(box.maxZ);
    }

    /**
     * Returns whether or not the entity intersects with any visible chunks in the graph.
     * @return True if the entity is visible, otherwise false
     */
    public boolean isEntityVisible(Entity entity) {
        if (!this.useEntityCulling) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (this.client.shouldEntityAppearGlowing(entity) || entity.shouldShowName()) {
            return true;
        }

        AABB box = entity.getBoundingBoxForCulling();


        if (isInfiniteExtentsBox(box)) {
            return true;
        }

        // bail on very large entities to avoid checking many sections
        double entityVolume = (box.maxX - box.minX) * (box.maxY - box.minY) * (box.maxZ - box.minZ);
        if (entityVolume > MAX_ENTITY_CHECK_VOLUME) {
            // TODO: do a frustum check instead, even large entities aren't visible if they're outside the frustum
            return true;
        }

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }

    public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
        // Boxes outside the valid world height will never map to a rendered chunk
        // Always render these boxes or they'll be culled incorrectly!
        if (y2 < this.world.getMinBuildHeight() + 0.5D || y1 > this.world.getMaxBuildHeight() - 0.5D) {
            return true;
        }

        int minX = SectionPos.posToSectionCoord(x1 - 0.5D);
        int minY = SectionPos.posToSectionCoord(y1 - 0.5D);
        int minZ = SectionPos.posToSectionCoord(z1 - 0.5D);

        int maxX = SectionPos.posToSectionCoord(x2 + 0.5D);
        int maxY = SectionPos.posToSectionCoord(y2 + 0.5D);
        int maxZ = SectionPos.posToSectionCoord(z2 + 0.5D);

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.renderSectionManager.isSectionVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public String getChunksDebugString() {
        // C: visible/total D: distance
        // TODO: add dirty and queued counts
        return String.format("C: %d/%d D: %d", this.renderSectionManager.getVisibleChunkCount(), this.renderSectionManager.getTotalSections(), this.renderDistance);
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified block region.
     */
    public void scheduleRebuildForBlockArea(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        this.scheduleRebuildForChunks(minX >> 4, minY >> 4, minZ >> 4, maxX >> 4, maxY >> 4, maxZ >> 4, important);
    }

    /**
     * Schedules chunk rebuilds for all chunks in the specified chunk region.
     */
    public void scheduleRebuildForChunks(int minX, int minY, int minZ, int maxX, int maxY, int maxZ, boolean important) {
        for (int chunkX = minX; chunkX <= maxX; chunkX++) {
            for (int chunkY = minY; chunkY <= maxY; chunkY++) {
                for (int chunkZ = minZ; chunkZ <= maxZ; chunkZ++) {
                    this.scheduleRebuildForChunk(chunkX, chunkY, chunkZ, important);
                }
            }
        }
    }

    /**
     * Schedules a chunk rebuild for the render belonging to the given chunk section position.
     */
    public void scheduleRebuildForChunk(int x, int y, int z, boolean important) {
        this.renderSectionManager.scheduleRebuild(x, y, z, important);
    }

    public Collection<String> getDebugStrings() {
        return this.renderSectionManager.getDebugStrings();
    }

    public boolean isSectionReady(int x, int y, int z) {
        return this.renderSectionManager.isSectionBuilt(x, y, z);
    }

    // Legacy compatibility
    @Deprecated
    public void onChunkAdded(int x, int z) {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_BLOCK_DATA);
    }

    @Deprecated
    public void onChunkLightAdded(int x, int z) {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.onChunkStatusAdded(x, z, ChunkStatus.FLAG_HAS_LIGHT_DATA);
    }

    @Deprecated
    public void onChunkRemoved(int x, int z) {
        var tracker = ChunkTrackerHolder.get(this.world);
        tracker.onChunkStatusRemoved(x, z, ChunkStatus.FLAG_ALL);
    }
}
