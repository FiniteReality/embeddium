package me.jellysquid.mods.sodium.client.render.pipeline.context;

import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.cache.ArrayLightDataCache;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorBlender;
import me.jellysquid.mods.sodium.client.render.pipeline.BlockRenderer;
import me.jellysquid.mods.sodium.client.render.pipeline.ChunkRenderCache;
import me.jellysquid.mods.sodium.client.render.pipeline.FluidRenderer;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.cloned.ChunkRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.BlockModelShaper;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.Level;
import org.embeddedt.embeddium.render.EmbeddiumRenderLayerCache;
import org.embeddedt.embeddium.render.world.WorldSliceLocalGenerator;

public class ChunkRenderCacheLocal extends ChunkRenderCache {
    private final ArrayLightDataCache lightDataCache;

    private final BlockRenderer blockRenderer;
    private final FluidRenderer fluidRenderer;

    private final BlockModelShaper blockModels;
    private final WorldSlice worldSlice;
    private BlockAndTintGetter localSlice;

    private final EmbeddiumRenderLayerCache renderLayerCache;

    public ChunkRenderCacheLocal(Minecraft client, Level world) {
        this.worldSlice = new WorldSlice(world);
        this.lightDataCache = new ArrayLightDataCache(this.worldSlice);

        LightPipelineProvider lightPipelineProvider = new LightPipelineProvider(this.lightDataCache);
        ColorBlender colorBlender = this.createBiomeColorBlender();

        this.blockRenderer = new BlockRenderer(client, lightPipelineProvider, colorBlender);
        this.fluidRenderer = new FluidRenderer(lightPipelineProvider, colorBlender);

        this.blockModels = client.getModelManager().getBlockModelShaper();

        this.renderLayerCache = new EmbeddiumRenderLayerCache();
    }

    public BlockModelShaper getBlockModels() {
        return this.blockModels;
    }

    public BlockRenderer getBlockRenderer() {
        return this.blockRenderer;
    }

    public FluidRenderer getFluidRenderer() {
        return this.fluidRenderer;
    }

    public EmbeddiumRenderLayerCache getRenderLayerCache() {
        return this.renderLayerCache;
    }

    public void init(ChunkRenderContext context) {
        this.lightDataCache.reset(context.getOrigin());
        this.worldSlice.copyData(context);
        // create the new local slice here so that it's unique whenever we copy new data
        // this is passed into mod code, since some depend on the provided BlockRenderView object being unique each time
        this.localSlice = WorldSliceLocalGenerator.generate(this.worldSlice);
    }

    public WorldSlice getWorldSlice() {
        return this.worldSlice;
    }

    public BlockAndTintGetter getLocalSlice() {
        return this.localSlice;
    }
}
