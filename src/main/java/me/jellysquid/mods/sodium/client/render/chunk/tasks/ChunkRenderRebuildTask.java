package me.jellysquid.mods.sodium.client.render.chunk.tasks;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.compat.FlywheelCompat;
import me.jellysquid.mods.sodium.client.compat.immersive.ImmersiveConnectionRenderer;
import me.jellysquid.mods.sodium.client.gl.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
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
import net.coderbot.iris.compat.sodium.impl.block_id.ChunkBuildBuffersExt;
import net.minecraft.block.BlockRenderType;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.chunk.ChunkOcclusionDataBuilder;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.EmptyModelData;
import net.minecraftforge.client.model.data.IModelData;

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

    public ChunkRenderRebuildTask(RenderSection render, ChunkRenderContext renderContext, int frame) {
        this.render = render;
        this.renderContext = renderContext;
        this.frame = frame;
    }

    @Override
    public ChunkBuildResult performBuild(ChunkBuildContext buildContext, CancellationSource cancellationSource) {
        ChunkRenderData.Builder renderData = new ChunkRenderData.Builder();
        ChunkOcclusionDataBuilder occluder = new ChunkOcclusionDataBuilder();
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

        Map<BlockPos, IModelData> modelDataMap = ModelDataManager.getModelData(MinecraftClient.getInstance().world, new ChunkPos(ChunkSectionPos.getSectionCoord(minX), ChunkSectionPos.getSectionCoord(minZ)));

        BlockPos.Mutable blockPos = new BlockPos.Mutable();
        BlockPos.Mutable offset = new BlockPos.Mutable();

        for (int y = minY; y < maxY; y++) {
            if (cancellationSource.isCancelled()) {
                return null;
            }

            for (int z = minZ; z < maxZ; z++) {
                for (int x = minX; x < maxX; x++) {
                    BlockState blockState = slice.getBlockState(x, y, z);

                    if (blockState.isAir()) {
                        continue;
                    }

                    blockPos.set(x, y, z);
                    offset.set(x & 15, y & 15, z & 15);

                    boolean rendered = false;

                    if (blockState.getRenderType() == BlockRenderType.MODEL) {
                        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
                            if (!RenderLayers.canRenderInLayer(blockState, layer)) {
                                continue;
                            }

                            ForgeHooksClient.setRenderType(layer);
                            IModelData modelData = modelDataMap.getOrDefault(blockPos, EmptyModelData.INSTANCE);

                            if (SodiumClientMod.oculusLoaded && buildContext.buffers instanceof ChunkBuildBuffersExt) {
                                ((ChunkBuildBuffersExt) buildContext.buffers).iris$setMaterialId(blockState, (short) -1);
                            }

                            BakedModel model = cache.getBlockModels()
                                    .getModel(blockState);

                            long seed = blockState.getRenderingSeed(blockPos);

                            if (cache.getBlockRenderer().renderModel(slice, blockState, blockPos, offset, model, buffers.get(layer), true, seed, modelData)) {
                                rendered = true;
                            }
                    	}
                    }

                    FluidState fluidState = blockState.getFluidState();

                    if (!fluidState.isEmpty()) {
                        for (RenderLayer layer : RenderLayer.getBlockLayers()) {
                            if (!RenderLayers.canRenderInLayer(fluidState, layer)) {
                                continue;
                            }
                            
                            if (SodiumClientMod.oculusLoaded && buildContext.buffers instanceof ChunkBuildBuffersExt) {
                                // All fluids have a ShadersMod render type of 1, to match behavior of Minecraft 1.7 and earlier.
                                ((ChunkBuildBuffersExt) buildContext.buffers).iris$setMaterialId(fluidState.getBlockState(), (short) 1);
                            }

                            ForgeHooksClient.setRenderType(layer);

                            if (cache.getFluidRenderer().render(slice, fluidState, blockPos, offset, buffers.get(layer))) {
                                rendered = true;
                            }
                        }
                    }

                    if (blockState.hasBlockEntity()) {
                        BlockEntity entity = slice.getBlockEntity(blockPos);

                        if (entity != null) {
                            BlockEntityRenderer<BlockEntity> renderer = MinecraftClient.getInstance().getBlockEntityRenderDispatcher().get(entity);

                            if (renderer != null && FlywheelCompat.addAndFilterBEs(entity)) {
                                renderData.addBlockEntity(entity, !renderer.rendersOutsideBoundingBox(entity));
                                rendered = true;
                            }
                        }
                    }

                    if (blockState.isOpaqueFullCube(slice, blockPos)) {
                        occluder.markClosed(blockPos);
                    }

                    if (rendered) {
                        bounds.addBlock(x & 15, y & 15, z & 15);
                    }
                }
            }
        }

        ForgeHooksClient.setRenderType(null);

        Map<BlockRenderPass, ChunkMeshData> meshes = new EnumMap<>(BlockRenderPass.class);
        
        if(SodiumClientMod.immersiveLoaded)
	        ImmersiveConnectionRenderer.renderConnectionsInSection(
	                buildContext.buffers, buildContext.cache.getWorldSlice(), render.getChunkPos()
	        );

        for (BlockRenderPass pass : BlockRenderPass.VALUES) {
            ChunkMeshData mesh = buffers.createMesh(pass);

            if (mesh != null) {
                meshes.put(pass, mesh);
            }
        }

        renderData.setOcclusionData(occluder.build());
        renderData.setBounds(bounds.build(this.render.getChunkPos()));

        return new ChunkBuildResult(this.render, renderData.build(), meshes, this.frame);
    }
}
