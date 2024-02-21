package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import java.util.Map;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkGraphicsState;
import me.jellysquid.mods.sodium.client.render.chunk.ChunkRenderContainer;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildResult;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkMeshData;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderBounds;
import me.jellysquid.mods.sodium.client.render.chunk.data.ChunkRenderData;
import me.jellysquid.mods.sodium.client.render.chunk.passes.BlockRenderPass;
import me.jellysquid.mods.sodium.client.render.pipeline.context.ChunkRenderCacheLocal;
import me.jellysquid.mods.sodium.client.util.task.CancellationSource;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import net.minecraft.ReportedException;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderDispatcher;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;
import org.embeddedt.embeddium.model.ModelDataSnapshotter;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkRenderRebuildTask<T extends ChunkGraphicsState> extends ChunkRenderBuildTask<T> {
    private final ChunkRenderContainer<T> render;
        
    private final BlockPos offset;

    private final ChunkRenderContext context;

    private final Map<BlockPos, IModelData> modelDataMap;

    private Vec3 camera;

    private final boolean translucencySorting;

    public ChunkRenderRebuildTask(ChunkRenderContainer<T> render, ChunkRenderContext context, BlockPos offset) {
        this.render = render;
        this.offset = offset;
        this.context = context;
        this.camera = Vec3.ZERO;
        this.translucencySorting = SodiumClientMod.options().advanced.translucencySorting;

        this.modelDataMap = ModelDataSnapshotter.getModelDataForSection(Minecraft.getInstance().level, this.context.getOrigin());
    }

    public ChunkRenderRebuildTask<T> withCameraPosition(Vec3 camera) {
        this.camera = camera;
        return this;
    }

    @Override
    public ChunkBuildResult<T> performBuild(ChunkRenderCacheLocal cache, ChunkBuildBuffers buffers, CancellationSource cancellationSource) {
        // COMPATIBLITY NOTE: Oculus relies on the LVT of this method being unchanged, at least in 16.5
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        VisGraph occluder = new VisGraph();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        buffers.init(renderData);

        cache.init(this.context);

        WorldSlice slice = cache.getWorldSlice();

        int baseX = this.render.getOriginX();
        int baseY = this.render.getOriginY();
        int baseZ = this.render.getOriginZ();

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        BlockPos renderOffset = this.offset;

        try {
            for (int relY = 0; relY < 16; relY++) {
                if (cancellationSource.isCancelled()) {
                    return null;
                }

                for (int relZ = 0; relZ < 16; relZ++) {
                    for (int relX = 0; relX < 16; relX++) {
                        BlockState blockState = slice.getBlockStateRelative(relX + 16, relY + 16, relZ + 16);

                        if (blockState.getBlock() == Blocks.AIR || blockState.getBlock() == Blocks.CAVE_AIR) {
                            continue;
                        }

                        // TODO: commit this separately
                        pos.set(baseX + relX, baseY + relY, baseZ + relZ);
                        buffers.setRenderOffset(pos.getX() - renderOffset.getX(), pos.getY() - renderOffset.getY(), pos.getZ() - renderOffset.getZ());

                        if (blockState.getRenderShape() == RenderShape.MODEL) {
                            for (RenderType layer : cache.getRenderLayerCache().forState(blockState)) {
                                ForgeHooksClient.setRenderLayer(layer);
                                IModelData modelData = modelDataMap.getOrDefault(pos, EmptyModelData.INSTANCE);

                                BakedModel model = cache.getBlockModels()
                                        .getBlockModel(blockState);

                                long seed = blockState.getSeed(pos);

                                if (cache.getBlockRenderer().renderModel(cache.getLocalSlice(), blockState, pos, model, buffers.get(layer), true, seed, modelData)) {
                                    bounds.addBlock(relX, relY, relZ);
                                }

                            }
                        }

                        FluidState fluidState = blockState.getFluidState();

                        if (!fluidState.isEmpty()) {
                            for (RenderType layer : cache.getRenderLayerCache().forState(fluidState)) {
                                ForgeHooksClient.setRenderLayer(layer);

                                if (cache.getFluidRenderer().render(cache.getLocalSlice(), fluidState, pos, buffers.get(layer))) {
                                    bounds.addBlock(relX, relY, relZ);
                                }
                            }
                        }

                        if (blockState.hasTileEntity()) {
                            BlockEntity entity = slice.getBlockEntity(pos);

                            if (entity != null) {
                                BlockEntityRenderer<BlockEntity> renderer = BlockEntityRenderDispatcher.instance.getRenderer(entity);

                                if (renderer != null) {
                                    renderData.addBlockEntity(entity, !renderer.shouldRenderOffScreen(entity));

                                    bounds.addBlock(relX, relY, relZ);
                                }
                            }
                        }

                        if (blockState.isSolidRender(slice, pos)) {
                            occluder.setOpaque(pos);
                        }
                    }
                }
            }
        } catch (ReportedException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getReport(), slice, pos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.forThrowable(ex, "Encountered exception while building chunk meshes"), slice, pos);
        }

        
        ForgeHooksClient.setRenderLayer(null);

        render.setRebuildForTranslucents(false);
        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass, (float)camera.x - offset.getX(), (float)camera.y - offset.getY(), (float)camera.z - offset.getZ(), this.translucencySorting);

            if (mesh != null) {
                renderData.setMesh(pass, mesh);
                if(this.translucencySorting && pass.isTranslucent())
                    render.setRebuildForTranslucents(true);
            }
        }

        renderData.setOcclusionData(occluder.resolve());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        ChunkDataBuiltEvent.BUS.post(new ChunkDataBuiltEvent(renderData));

        return new ChunkBuildResult<>(this.render, renderData.build());
    }

    private ReportedException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.addCategory("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.populateBlockDetails(crashReportSection, pos, state);

        crashReportSection.setDetail("Chunk section", render);
        if (context != null) {
            crashReportSection.setDetail("Render context volume", context.getVolume());
        }

        return new ReportedException(report);
    }

    @Override
    public void releaseResources() {
        this.context.releaseResources();
    }
}
