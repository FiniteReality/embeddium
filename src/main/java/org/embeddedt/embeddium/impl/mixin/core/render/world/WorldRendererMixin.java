package org.embeddedt.embeddium.impl.mixin.core.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.client.renderer.*;
import org.embeddedt.embeddium.impl.gl.device.RenderDevice;
import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
import org.embeddedt.embeddium.impl.render.viewport.ViewportProvider;
import org.embeddedt.embeddium.impl.world.WorldRendererExtended;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.BlockDestructionProgress;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.client.ClientHooks;
import org.embeddedt.embeddium.impl.sodium.FlawlessFrames;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.SortedSet;
import java.util.function.Consumer;

@Mixin(LevelRenderer.class)
public abstract class WorldRendererMixin implements WorldRendererExtended {
    @Shadow
    @Final
    private RenderBuffers renderBuffers;

    @Shadow
    @Final
    private Long2ObjectMap<SortedSet<BlockDestructionProgress>> destructionProgress;

    @Shadow
    private int ticks;

    @Shadow
    @Final
    private Minecraft minecraft;

    @Unique
    private EmbeddiumWorldRenderer renderer;

    @Unique
    private int frame;

    @Shadow public abstract boolean shouldShowEntityOutlines();

    @Shadow
    @Nullable
    private ClientLevel level;

    @Shadow
    @Final
    private SectionOcclusionGraph sectionOcclusionGraph;

    @Shadow
    private Frustum cullingFrustum;

    @Override
    public EmbeddiumWorldRenderer sodium$getWorldRenderer() {
        return this.renderer;
    }

    @Redirect(method = "allChanged()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Options;getEffectiveRenderDistance()I", ordinal = 1))
    private int nullifyBuiltChunkStorage(Options options) {
        // Do not allow any resources to be allocated
        return 0;
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void init(Minecraft client, EntityRenderDispatcher entityRenderDispatcher, BlockEntityRenderDispatcher blockEntityRenderDispatcher, RenderBuffers bufferBuilderStorage, CallbackInfo ci) {
        this.renderer = new EmbeddiumWorldRenderer(client);
    }

    @Inject(method = "setLevel", at = @At("RETURN"))
    private void onWorldChanged(ClientLevel world, CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.setWorld(world);
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect to our renderer
     * @author JellySquid
     */
    @Overwrite
    public int countRenderedSections() {
        return this.renderer.getVisibleChunkCount();
    }

    /**
     * @reason Redirect to our renderer
     * @author embeddedt
     */
    @Overwrite
    public void iterateVisibleBlockEntities(Consumer<BlockEntity> blockEntityConsumer) {
        this.renderer.forEachVisibleBlockEntity(blockEntityConsumer);
    }

    /**
     * @reason Redirect the check to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean hasRenderedAllSections() {
        return this.renderer.isTerrainRenderComplete();
    }

    @Inject(method = "needsUpdate", at = @At("RETURN"))
    private void onTerrainUpdateScheduled(CallbackInfo ci) {
        this.renderer.scheduleTerrainUpdate();
    }

    /**
     * @reason Redirect the chunk layer render passes to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void renderSectionLayer(RenderType renderLayer, double x, double y, double z, Matrix4f pose, Matrix4f matrix) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.drawChunkLayer(renderLayer, pose, x, y, z);
        } finally {
            RenderDevice.exitManagedCode();
        }

        // TODO: Avoid setting up and clearing the state a second time
        renderLayer.setupRenderState();
        ClientHooks.dispatchRenderStage(renderLayer, ((LevelRenderer)(Object)this), pose, matrix, this.ticks, this.minecraft.gameRenderer.getMainCamera(), this.cullingFrustum);
        renderLayer.clearRenderState();
    }

    /**
     * @reason Redirect the terrain setup phase to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setupRender(Camera camera, Frustum frustum, boolean hasForcedFrustum, boolean spectator) {
        var viewport = ((ViewportProvider) frustum).sodium$createViewport();

        // Detect mods setting the vanilla update flags themselves
        if (this.sectionOcclusionGraph.consumeFrustumUpdate()) {
            this.renderer.scheduleTerrainUpdate();
        }

        RenderDevice.enterManagedCode();

        try {
            this.renderer.setupTerrain(camera, viewport, this.frame++, spectator, FlawlessFrames.isActive());
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setBlocksDirty(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        this.renderer.scheduleRebuildForBlockArea(minX, minY, minZ, maxX, maxY, maxZ, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public void setSectionDirtyWithNeighbors(int x, int y, int z) {
        this.renderer.scheduleRebuildForChunks(x - 1, y - 1, z - 1, x + 1, y + 1, z + 1, false);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setBlockDirty(BlockPos pos, boolean important) {
        this.renderer.scheduleRebuildForBlockArea(pos.getX() - 1, pos.getY() - 1, pos.getZ() - 1, pos.getX() + 1, pos.getY() + 1, pos.getZ() + 1, important);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    private void setSectionDirty(int x, int y, int z, boolean important) {
        this.renderer.scheduleRebuildForChunk(x, y, z, important);
    }

    /**
     * @reason Redirect chunk updates to our renderer
     * @author JellySquid
     */
    @Overwrite
    public boolean isSectionCompiled(BlockPos pos) {
        return this.renderer.isSectionReady(pos.getX() >> 4, pos.getY() >> 4, pos.getZ() >> 4);
    }

    @Inject(method = "allChanged()V", at = @At("RETURN"))
    private void onReload(CallbackInfo ci) {
        RenderDevice.enterManagedCode();

        try {
            this.renderer.reload();
        } finally {
            RenderDevice.exitManagedCode();
        }
    }

    /**
     * @author embeddedt
     * @reason take over block entity rendering
     */
    @Overwrite
    private void renderBlockEntities(PoseStack stack, MultiBufferSource.BufferSource bufferSource, MultiBufferSource.BufferSource bufferSource2, Camera camera, float partialTick) {
        this.renderer.renderBlockEntities(stack.last().pose(), this.renderBuffers, this.destructionProgress, camera, partialTick);
    }

    /**
     * Target the flag that selects whether or not to enable the entity outline shader, and enable it if
     * we rendered a block entity that requested it.
     *
     * NOTE: When updating Embeddium to newer versions of the game, this injection point must be checked.
     */
    // TODO 24w33a
    /*
    @ModifyVariable(method = "renderLevel", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/LevelRenderer;globalBlockEntities:Ljava/util/Set;", shift = At.Shift.BEFORE, ordinal = 0), ordinal = 3)
    private boolean changeEntityOutlineFlag(boolean bl) {
        return bl || (this.renderer.didBlockEntityRequestOutline() && this.shouldShowEntityOutlines());
    }
     */

    /**
     * @reason Replace the debug string
     * @author JellySquid
     */
    @Overwrite
    public String getSectionStatistics() {
        return this.renderer.getChunksDebugString();
    }
}
