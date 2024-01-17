package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.device.CommandList;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderMatrices;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkTracker;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSectionManager;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheShared;
import me.jellysquid.mods.sodium.client.util.NativeBuffer;
import me.jellysquid.mods.sodium.client.util.frustum.Frustum;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import me.jellysquid.mods.sodium.common.util.ListUtil;
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
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.extensions.IForgeBlockEntity;

import java.util.Collection;
import java.util.Set;
import java.util.SortedSet;

/**
 * Provides an extension to vanilla's {@link LevelRenderer}.
 */
public class SodiumWorldRenderer {
    private final Minecraft client;

    private ClientLevel world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    private Frustum frustum;

    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private RenderSectionManager renderSectionManager;
    private BlockRenderPassManager renderPassManager;
    private ChunkTracker chunkTracker;

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
            return ((WorldRendererExtended) world).getSodiumWorldRenderer();
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
        this.chunkTracker = new ChunkTracker();

        ChunkRenderCacheShared.createRenderContext(this.world);

        try (CommandList commandList = RenderDevice.INSTANCE.createCommandList()) {
            this.initRenderer(commandList);
        }
    }

    private void unloadWorld() {
        ChunkRenderCacheShared.destroyRenderContext(this.world);

        if (this.renderSectionManager != null) {
            this.renderSectionManager.destroy();
            this.renderSectionManager = null;
        }

        this.globalBlockEntities.clear();

        this.chunkTracker = null;
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
    public void updateChunks(Camera camera, Frustum frustum, @Deprecated(forRemoval = true) int frame, boolean spectator) {
        NativeBuffer.reclaim(false);

        this.frustum = frustum;

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

        this.lastCameraX = pos.x;
        this.lastCameraY = pos.y;
        this.lastCameraZ = pos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;
        this.lastFogDistance = fogDistance;

        profiler.popPush("chunk_update");

        this.chunkTracker.update();
        this.renderSectionManager.updateChunks();

        if (this.renderSectionManager.isGraphDirty()) {
            profiler.popPush("chunk_graph_rebuild");

            this.renderSectionManager.update(camera, frustum, frame, spectator);
        }

        profiler.popPush("visible_chunk_tick");

        this.renderSectionManager.tickVisibleRenders();

        profiler.pop();

        Entity.setViewScale(Mth.clamp((double) this.client.options.getEffectiveRenderDistance() / 8.0D, 1.0D, 2.5D) * this.client.options.entityDistanceScaling().get());
    }

    /**
     * Performs a render pass for the given {@link RenderType} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderType renderLayer, PoseStack matrixStack, double x, double y, double z) {
        BlockRenderPass pass = this.renderPassManager.getRenderPassForLayer(renderLayer);
        pass.startDrawing();

        this.renderSectionManager.renderLayer(ChunkRenderMatrices.from(matrixStack), pass, x, y, z);

        pass.endDrawing();
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

        this.globalBlockEntities.clear();

        this.renderPassManager = BlockRenderPassManager.createDefaultMappings();

        this.renderSectionManager = new RenderSectionManager(this, this.renderPassManager, this.world, this.renderDistance, commandList);
        this.renderSectionManager.reloadChunks(this.chunkTracker);
    }

    private boolean checkBEVisibility(BlockEntity entity) {
        AABB box = entity.getRenderBoundingBox();
        if(box.equals(IForgeBlockEntity.INFINITE_EXTENT_AABB))
            return true;
        return frustum.isBoxVisible((float)box.minX, (float)box.minY, (float)box.minZ, (float)box.maxX, (float)box.maxY, (float)box.maxZ);
    }

    // We track whether a block entity uses custom block outline rendering, so that the outline postprocessing
    // shader will be enabled appropriately
    private boolean blockEntityRequestedOutline;

    public boolean didBlockEntityRequestOutline() {
        return blockEntityRequestedOutline;
    }

    public void renderTileEntities(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                   Camera camera, float tickDelta) {

        MultiBufferSource.BufferSource immediate = bufferBuilders.bufferSource();

        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x();
        double y = cameraPos.y();
        double z = cameraPos.z();

        BlockEntityRenderDispatcher blockEntityRenderer = Minecraft.getInstance().getBlockEntityRenderDispatcher();

        this.blockEntityRequestedOutline = false;

        for (BlockEntity blockEntity : this.renderSectionManager.getVisibleBlockEntities()) {
            if(!checkBEVisibility(blockEntity))
                continue;
            BlockPos pos = blockEntity.getBlockPos();

            matrices.pushPose();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            MultiBufferSource consumer = immediate;
            SortedSet<BlockDestructionProgress> breakingInfos = blockBreakingProgressions.get(pos.asLong());

            if (breakingInfos != null && !breakingInfos.isEmpty()) {
                int stage = breakingInfos.last().getProgress();

                if (stage >= 0) {
                    PoseStack.Pose entry = matrices.last();
                    VertexConsumer transformer = new SheetedDecalTextureGenerator(bufferBuilders.crumblingBufferSource().getBuffer(ModelBakery.DESTROY_TYPES.get(stage)), entry.pose(), entry.normal());
                    consumer = (layer) -> layer.affectsCrumbling() ? VertexMultiConsumer.create(transformer, immediate.getBuffer(layer)) : immediate.getBuffer(layer);
                }
            }

            if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                this.blockEntityRequestedOutline = true;
            }

            try {
                blockEntityRenderer.render(blockEntity, tickDelta, matrices, consumer);
            } catch(RuntimeException e) {
                // We catch errors from removed block entities here, because we often end up being faster
                // than vanilla, and rendering them when they wouldn't be rendered by vanilla, which can
                // cause crashes. However, we do not apply this suppression to regular rendering.
                if (!blockEntity.isRemoved()) {
                    throw e;
                } else {
                    SodiumClientMod.logger().error("Suppressing crash from removed block entity", e);
                }
            }

            matrices.popPose();
        }

        for (BlockEntity blockEntity : this.globalBlockEntities) {
            if(!checkBEVisibility(blockEntity))
                continue;

            BlockPos pos = blockEntity.getBlockPos();

            matrices.pushPose();
            matrices.translate((double) pos.getX() - x, (double) pos.getY() - y, (double) pos.getZ() - z);

            if (blockEntity.hasCustomOutlineRendering(this.client.player)) {
                this.blockEntityRequestedOutline = true;
            }

            blockEntityRenderer.render(blockEntity, tickDelta, matrices, immediate);

            matrices.popPose();
        }
    }

    public void onChunkAdded(int x, int z) {
        if (this.chunkTracker.loadChunk(x, z)) {
            this.renderSectionManager.onChunkAdded(x, z);
        }
    }

    public void onChunkLightAdded(int x, int z) {
        this.chunkTracker.onLightDataAdded(x, z);
    }

    public void onChunkRemoved(int x, int z) {
        if (this.chunkTracker.unloadChunk(x, z)) {
            this.renderSectionManager.onChunkRemoved(x, z);
        }
    }

    public void onChunkRenderUpdated(int x, int y, int z, ChunkRenderData meshBefore, ChunkRenderData meshAfter) {
        ListUtil.updateList(this.globalBlockEntities, meshBefore.getGlobalBlockEntities(), meshAfter.getGlobalBlockEntities());

        this.renderSectionManager.onChunkRenderUpdates(x, y, z, meshAfter);
    }

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

        AABB box = entity.getBoundingBoxForCulling();

        // Entities outside the valid world height will never map to a rendered chunk
        // Always render these entities or they'll be culled incorrectly!
        if (box.maxY < 0.5D || box.minY > 255.5D) {
            return true;
        }

        if (isInfiniteExtentsBox(box)) {
            return true;
        }

        // Ensure entities with outlines or nametags are always visible
        if (this.client.shouldEntityAppearGlowing(entity) || entity.shouldShowName()) {
            return true;
        }

        return this.isBoxVisible(box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ);
    }
    
    public boolean doesChunkHaveFlag(int x, int z, int status) {
        return this.chunkTracker.hasMergedFlags(x, z, status);
    }
    
    public boolean isBoxVisible(double x1, double y1, double z1, double x2, double y2, double z2) {
        int minX = Mth.floor(x1 - 0.5D) >> 4;
        int minY = Mth.floor(y1 - 0.5D) >> 4;
        int minZ = Mth.floor(z1 - 0.5D) >> 4;

        int maxX = Mth.floor(x2 + 0.5D) >> 4;
        int maxY = Mth.floor(y2 + 0.5D) >> 4;
        int maxZ = Mth.floor(z2 + 0.5D) >> 4;

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
        // C: visible/total
        // TODO: add dirty and queued counts
        return String.format("C: %s/%s", this.renderSectionManager.getVisibleChunkCount(), this.renderSectionManager.getTotalSections());
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

    public Collection<String> getMemoryDebugStrings() {
        return this.renderSectionManager.getDebugStrings();
    }

    public ChunkTracker getChunkTracker() {
        return this.chunkTracker;
    }
}
