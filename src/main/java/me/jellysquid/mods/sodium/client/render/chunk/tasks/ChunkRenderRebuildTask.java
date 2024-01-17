package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBufferSorter;
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
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.chunk.VisGraph;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.common.MinecraftForge;
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;
import org.embeddedt.embeddium.chunk.MeshAppenderRenderer;
import org.embeddedt.embeddium.model.ModelDataSnapshotter;

import java.util.EnumMap;
import java.util.Map;

/**
 * Rebuilds all the meshes of a chunk for each given render pass with non-occluded blocks. The result is then uploaded
 * to graphics memory on the main thread.
 *
 * This task takes a slice of the world from the thread it is created on. Since these slices require rather large
 * array allocations, they are pooled to ensure that the garbage collector doesn't become overloaded.
 */
public class ChunkRenderRebuildTask extends ChunkRenderBuildTask {
    private final RenderSection render;
    private final ChunkRenderContext renderContext;
    private final int frame;
    private final Map<BlockPos, ModelData> modelDataMap;

    private Vec3 camera;

    private final boolean translucencySorting;

    public ChunkRenderRebuildTask(RenderSection render, ChunkRenderContext renderContext, int frame) {
        this.render = render;
        this.renderContext = renderContext;
        this.frame = frame;
        this.camera = Vec3.ZERO;
        this.translucencySorting = SodiumClientMod.options().performance.useTranslucentFaceSorting;

        this.modelDataMap = ModelDataSnapshotter.getModelDataForSection(Minecraft.getInstance().level, this.renderContext.getOrigin());
    }
    
    private final SingleThreadedRandomSource random = new SingleThreadedRandomSource(42L);

    public ChunkRenderRebuildTask withCameraPosition(Vec3 camera) {
        this.camera = camera;
        return this;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource) {
        // COMPATIBLITY NOTE: Oculus relies on the LVT of this method being unchanged, at least in 16.5
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        VisGraph occluder = new VisGraph();
        ChunkRenderBounds.Builder bounds = new ChunkRenderBounds.Builder();

        ChunkBuildBuffers buffers = buildContext.buffers;
        buffers.init(renderData, this.render.getChunkId());

        ChunkRenderCacheLocal cache = buildContext.cache;
        cache.init(this.renderContext);

        WorldSlice slice = cache.getWorldSlice();

        int minX = this.render.getOriginX();
        int minY = this.render.getOriginY();
        int minZ = this.render.getOriginZ();

        int maxX = minX + 16;
        int maxY = minY + 16;
        int maxZ = minZ + 16;

        Map<BlockPos, ModelData> modelDataMap = this.modelDataMap;

        // Initialise with minX/minY/minZ so initial getBlockState crash context is correct
        BlockPos.MutableBlockPos blockPos = new BlockPos.MutableBlockPos(minX, minY, minZ);
        BlockPos.MutableBlockPos offset = new BlockPos.MutableBlockPos();

        try {
            for (int y = minY; y < maxY; y++) {
                if (cancellationSource.isCancelled()) {
                    return null;
                }

                for (int z = minZ; z < maxZ; z++) {
                    for (int x = minX; x < maxX; x++) {
                        BlockState blockState = slice.getBlockState(x, y, z);

                        if (blockState.isAir() && !blockState.hasBlockEntity()) {
                            continue;
                        }

                        blockPos.set(x, y, z);
                        offset.set(x & 15, y & 15, z & 15);

                        boolean rendered = false;

                        if (blockState.getRenderShape() == RenderShape.MODEL) {
                            BakedModel model = cache.getBlockModels()
                                    .getBlockModel(blockState);
                            ModelData modelData = modelDataMap.getOrDefault(blockPos, ModelData.EMPTY);
                            modelData = model.getModelData(cache.getLocalSlice(), blockPos, blockState, modelData);
                            random.setSeed(blockState.getSeed(blockPos));
                            for (RenderType layer : model.getRenderTypes(blockState, random, modelData)) {
                                long seed = blockState.getSeed(blockPos);

                                if (cache.getBlockRenderer().renderModel(cache.getLocalSlice(), blockState, blockPos, offset, model, buffers.get(layer), true, seed, modelData, layer, random)) {
                                    rendered = true;
                                }
                            }
                        }

                        FluidState fluidState = blockState.getFluidState();

                        if (!fluidState.isEmpty()) {
                            RenderType layer = ItemBlockRenderTypes.getRenderLayer(fluidState);

                            if (cache.getFluidRenderer().render(cache.getLocalSlice(), fluidState, blockPos, offset, buffers.get(layer))) {
                                rendered = true;
                            }
                        }

                        if (blockState.hasBlockEntity()) {
                            BlockEntity entity = slice.getBlockEntity(blockPos);

                            if (entity != null) {
                                BlockEntityRenderer<BlockEntity> renderer = Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(entity);

                                if (renderer != null) {
                                    renderData.addBlockEntity(entity, !renderer.shouldRenderOffScreen(entity));
                                    rendered = true;
                                }
                            }
                        }

                        if (blockState.isSolidRender(slice, blockPos)) {
                            occluder.setOpaque(blockPos);
                        }

                        if (rendered) {
                            bounds.addBlock(x & 15, y & 15, z & 15);
                        }
                    }
                }
            }

            MeshAppenderRenderer.renderMeshAppenders(renderContext.getMeshAppenders(), cache.getLocalSlice(), render.getChunkPos(), buffers);
        } catch (ReportedException ex) {
            // Propagate existing crashes (add context)
            throw fillCrashInfo(ex.getReport(), slice, blockPos);
        } catch (Throwable ex) {
            // Create a new crash report for other exceptions (e.g. thrown in getQuads)
            throw fillCrashInfo(CrashReport.forThrowable(ex, "Encountered exception while building chunk meshes"), slice, blockPos);
        }

        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass);

            if (mesh != null) {
                if (this.translucencySorting && pass.isTranslucent())
                    ChunkBufferSorter.sort(ChunkBufferSorter.SortBuffer.wrap(mesh), (float)camera.x - minX, (float)camera.y - minY, (float)camera.z - minZ);

                meshes.put(pass, mesh);
            }
        }

        renderData.setOcclusionData(occluder.resolve());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        MinecraftForge.EVENT_BUS.post(new ChunkDataBuiltEvent(renderData));

        return new ChunkBuildResult(this.render, renderData.build(), meshes, this.frame);
    }

    private ReportedException fillCrashInfo(CrashReport report, WorldSlice slice, BlockPos pos) {
        CrashReportCategory crashReportSection = report.addCategory("Block being rendered", 1);

        BlockState state = null;
        try {
            state = slice.getBlockState(pos);
        } catch (Exception ignored) {}
        CrashReportCategory.populateBlockDetails(crashReportSection, slice, pos, state);

        crashReportSection.setDetail("Chunk section", render);
        if (renderContext != null) {
            crashReportSection.setDetail("Render context volume", renderContext.getVolume());
        }

        return new ReportedException(report);
    }
    
    @Override
    public void releaseResources() {
        this.renderContext.releaseResources();
    }
}
