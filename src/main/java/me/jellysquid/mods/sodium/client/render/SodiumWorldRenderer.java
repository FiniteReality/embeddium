package me.jellysquid.mods.sodium.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.SheetedDecalTextureGenerator;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexMultiConsumer;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.compat.FogHelper;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.vertex.type.ChunkVertexType;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderManager;
import me.jellysquid.mods.sodium.client.render.chunk.backends.multidraw.MultidrawChunkRenderBackend;
import me.jellysquid.mods.sodium.client.render.chunk.backends.oneshot.ChunkRenderBackendOneshot;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.format.DefaultModelVertexFormats;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPassManager;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheShared;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListener;
import me.jellysquid.mods.sodium.client.world.ChunkStatusListenerManager;
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
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.resources.model.ModelBakery;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.util.Mth;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLLoader;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.SortedSet;

/**
 * Provides an extension to vanilla's {@link LevelRenderer}.
 */
public class SodiumWorldRenderer implements ChunkStatusListener {
    private static final boolean ENABLE_BLOCKENTITY_CULLING = FMLLoader.getLoadingModList().getModFileById("valkyrienskies") == null;

    private final Minecraft client;

    private ClientLevel world;
    private int renderDistance;

    private double lastCameraX, lastCameraY, lastCameraZ;
    private double lastCameraPitch, lastCameraYaw;
    private float lastFogDistance;

    private boolean useEntityCulling;

    private final LongSet loadedChunkPositions = new LongOpenHashSet();
    private final Set<BlockEntity> globalBlockEntities = new ObjectOpenHashSet<>();

    private Frustum frustum;
    private ChunkRenderManager<?> chunkRenderManager;
    private BlockRenderPassManager renderPassManager;
    private ChunkRenderBackend<?> chunkRenderBackend;

    /**
     * Instantiates Sodium's world renderer. This should be called at the time of the world renderer initialization.
     */
    public static SodiumWorldRenderer create() {
        return new SodiumWorldRenderer(Minecraft.getInstance());
    }

    /**
     * @throws IllegalStateException If the renderer has not yet been created
     * @return The current instance of this type
     */
    @Deprecated
    public static SodiumWorldRenderer getInstance() {
        SodiumWorldRenderer instance = getInstanceNullable();

        if (instance == null) {
            throw new IllegalStateException("Renderer not initialized");
        }

        return instance;
    }

    /**
     * @return The current instance of the Sodium terrain renderer, or null if the renderer is not active
     */
    public static SodiumWorldRenderer getInstanceNullable() {
        LevelRenderer world = Minecraft.getInstance().levelRenderer;

        if (world instanceof WorldRendererExtended) {
            return ((WorldRendererExtended) world).getSodiumWorldRenderer();
        }

        return null;
    }

    private SodiumWorldRenderer(Minecraft client) {
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

        ChunkRenderCacheShared.createRenderContext(this.world);

        this.initRenderer();

        ((ChunkStatusListenerManager) world.getChunkSource()).setListener(this);
    }

    private void unloadWorld() {
        ChunkRenderCacheShared.destroyRenderContext(this.world);

        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.loadedChunkPositions.clear();
        this.globalBlockEntities.clear();

        this.world = null;
    }

    /**
     * @return The number of chunk renders which are visible in the current camera's frustum
     */
    public int getVisibleChunkCount() {
        return this.chunkRenderManager.getVisibleChunkCount();
    }

    /**
     * Notifies the chunk renderer that the graph scene has changed and should be re-computed.
     */
    public void scheduleTerrainUpdate() {
        // BUG: seems to be called before init
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.markDirty();
        }
    }

    /**
     * @return True if no chunks are pending rebuilds
     */
    public boolean isTerrainRenderComplete() {
        return this.chunkRenderManager.isBuildComplete();
    }

    // We'll keep it to have compatibility with Oculus' older versions
    public static boolean hasChanges = false;
    
    /**
     * Called prior to any chunk rendering in order to update necessary state.
     */
    public void updateChunks(Camera camera, Frustum frustum, boolean hasForcedFrustum, int frame, boolean spectator) {
        this.frustum = frustum;

        this.useEntityCulling = SodiumClientMod.options().advanced.useEntityCulling;

        if (this.client.options.renderDistance != this.renderDistance) {
            this.reload();
        }

        ProfilerFiller profiler = this.client.getProfiler();
        profiler.push("camera_setup");

        LocalPlayer player = this.client.player;

        if (player == null) {
            throw new IllegalStateException("Client instance has no active player entity");
        }

        Vec3 pos = camera.getPosition();

        this.chunkRenderManager.setCameraPosition(pos.x, pos.y, pos.z);

        float pitch = camera.getXRot();
        float yaw = camera.getYRot();

        float fogDistance = FogHelper.getFogCutoff();

        boolean dirty = pos.x != this.lastCameraX || pos.y != this.lastCameraY || pos.z != this.lastCameraZ ||
                pitch != this.lastCameraPitch || yaw != this.lastCameraYaw || fogDistance != this.lastFogDistance;

        if (dirty) {
            this.chunkRenderManager.markDirty();
        }

        this.lastCameraX = pos.x;
        this.lastCameraY = pos.y;
        this.lastCameraZ = pos.z;
        this.lastCameraPitch = pitch;
        this.lastCameraYaw = yaw;
        this.lastFogDistance = fogDistance;

        profiler.popPush("chunk_update");

        this.chunkRenderManager.updateChunks();

        if (!hasForcedFrustum && this.chunkRenderManager.isDirty()) {
            profiler.popPush("chunk_graph_rebuild");

            this.chunkRenderManager.update(camera, (FrustumExtended) frustum, frame, spectator);
        }

        profiler.popPush("visible_chunk_tick");

        this.chunkRenderManager.tickVisibleRenders();

        profiler.pop();

        Entity.setViewScale(Mth.clamp((double) this.client.options.renderDistance / 8.0D, 1.0D, 2.5D) * (double) this.client.options.entityDistanceScaling);
    }

    /**
     * Performs a render pass for the given {@link RenderType} and draws all visible chunks for it.
     */
    public void drawChunkLayer(RenderType renderLayer, PoseStack matrixStack, double x, double y, double z) {
        BlockRenderPass pass = this.renderPassManager.getRenderPassForLayer(renderLayer);
        pass.startDrawing();

        this.chunkRenderManager.renderLayer(matrixStack, pass, x, y, z);

        pass.endDrawing();

        RenderSystem.clearCurrentColor();
    }

    public void reload() {
        if (this.world == null) {
            return;
        }

        this.initRenderer();
    }

    private void initRenderer() {
        if (this.chunkRenderManager != null) {
            this.chunkRenderManager.destroy();
            this.chunkRenderManager = null;
        }

        if (this.chunkRenderBackend != null) {
            this.chunkRenderBackend.delete();
            this.chunkRenderBackend = null;
        }

        this.globalBlockEntities.clear();

        RenderDevice device = RenderDevice.INSTANCE;

        this.renderDistance = this.client.options.renderDistance;

        SodiumGameOptions opts = SodiumClientMod.options();

        this.renderPassManager = BlockRenderPassManager.createDefaultMappings();

        final ChunkVertexType vertexFormat;

        if (opts.advanced.useCompactVertexFormat) {
            vertexFormat = DefaultModelVertexFormats.MODEL_VERTEX_HFP;
        } else {
            vertexFormat = DefaultModelVertexFormats.MODEL_VERTEX_SFP;
        }

        this.chunkRenderBackend = createChunkRenderBackend(device, opts, vertexFormat);
        this.chunkRenderBackend.createShaders(device);

        this.chunkRenderManager = new ChunkRenderManager<>(this, this.chunkRenderBackend, this.renderPassManager, this.world, this.renderDistance);
        this.chunkRenderManager.restoreChunks(this.loadedChunkPositions);
    }

    private static ChunkRenderBackend<?> createChunkRenderBackend(RenderDevice device,
                                                                  SodiumGameOptions options,
                                                                  ChunkVertexType vertexFormat) {
        boolean disableBlacklist = SodiumClientMod.options().advanced.ignoreDriverBlacklist;

        if (options.advanced.useChunkMultidraw && MultidrawChunkRenderBackend.isSupported(disableBlacklist)) {
            return new MultidrawChunkRenderBackend(device, vertexFormat);
        } else {
            return new ChunkRenderBackendOneshot(vertexFormat);
        }
    }

    private boolean checkBEVisibility(BlockEntity entity) {
        if(!ENABLE_BLOCKENTITY_CULLING) {
            return true;
        }
        return frustum.isVisible(entity.getRenderBoundingBox());
    }

    public void renderTileEntities(PoseStack matrices, RenderBuffers bufferBuilders, Long2ObjectMap<SortedSet<BlockDestructionProgress>> blockBreakingProgressions,
                                   Camera camera, float tickDelta) {
        MultiBufferSource.BufferSource immediate = bufferBuilders.bufferSource();

        Vec3 cameraPos = camera.getPosition();
        double x = cameraPos.x();
        double y = cameraPos.y();
        double z = cameraPos.z();

        for (BlockEntity blockEntity : this.chunkRenderManager.getVisibleBlockEntities()) {
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

            try {
                BlockEntityRenderDispatcher.instance.render(blockEntity, tickDelta, matrices, consumer);
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

            BlockEntityRenderDispatcher.instance.render(blockEntity, tickDelta, matrices, immediate);

            matrices.popPose();
        }
    }

    @Override
    public void onChunkAdded(int x, int z) {
        this.loadedChunkPositions.add(ChunkPos.asLong(x, z));
        this.chunkRenderManager.onChunkAdded(x, z);
    }

    @Override
    public void onChunkRemoved(int x, int z) {
        this.loadedChunkPositions.remove(ChunkPos.asLong(x, z));
        this.chunkRenderManager.onChunkRemoved(x, z);
    }

    public void onChunkRenderUpdated(int x, int y, int z, ChunkRenderData meshBefore, ChunkRenderData meshAfter) {
        ListUtil.updateList(this.globalBlockEntities, meshBefore.getGlobalBlockEntities(), meshAfter.getGlobalBlockEntities());
        
        this.chunkRenderManager.onChunkRenderUpdates(x, y, z, meshAfter);
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

        int minX = Mth.floor(box.minX - 0.5D) >> 4;
        int minY = Mth.floor(box.minY - 0.5D) >> 4;
        int minZ = Mth.floor(box.minZ - 0.5D) >> 4;

        int maxX = Mth.floor(box.maxX + 0.5D) >> 4;
        int maxY = Mth.floor(box.maxY + 0.5D) >> 4;
        int maxZ = Mth.floor(box.maxZ + 0.5D) >> 4;

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    if (this.chunkRenderManager.isChunkVisible(x, y, z)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    /**
     * @return The frustum of the current player's camera used to cull chunks
     */
    public Frustum getFrustum() {
        return this.frustum;
    }

    public String getChunksDebugString() {
        // C: visible/total
        // TODO: add dirty and queued counts
        return String.format("C: %s/%s", this.chunkRenderManager.getVisibleChunkCount(), this.chunkRenderManager.getTotalSections());
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
        this.chunkRenderManager.scheduleRebuild(x, y, z, important);
    }

    public ChunkRenderBackend<?> getChunkRenderer() {
        return this.chunkRenderBackend;
    }

    public Collection<ChunkRenderCacheLocal> getActiveChunkRenderCaches() {
        return this.chunkRenderManager != null ? this.chunkRenderManager.getActiveChunkRenderCaches() : Collections.emptyList();
    }
}
