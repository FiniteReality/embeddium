package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.embeddedt.embeddium.render.type.RenderTypeExtended;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

/**
 * Adaptation of Indigo's {@link BlockRenderContext} that delegates back to the Sodium renderer.
 */
public class IndigoBlockRenderContext extends BlockRenderContext {
    private final SinkingVertexBuilder[] vertexBuilderMap = new SinkingVertexBuilder[RenderType.chunkBufferLayers().size()];

    private me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext currentContext;
    private final BlockOcclusionCache occlusionCache;

    private int cullChecked, cullValue;

    public IndigoBlockRenderContext(BlockOcclusionCache occlusionCache) {
        this.occlusionCache = occlusionCache;
    }

    @Override
    public boolean isFaceCulled(@Nullable Direction face) {
        if (face == null) {
            return false;
        }

        int fM = (1 << face.ordinal());

        // Use a bitmask to cache the cull checks so we don't run them more than once per face
        if((cullChecked & fM) != 0) {
            return (cullValue & fM) != 0;
        } else {
            var ctx = this.currentContext;
            boolean flag = !this.occlusionCache.shouldDrawSide(ctx.state(), ctx.localSlice(), ctx.pos(), face);
            if(flag) {
                cullValue |= fM;
            }
            cullChecked |= fM;
            return flag;
        }
    }

    @Override
    protected VertexConsumer getVertexConsumer(RenderType layer) {
        int id = ((RenderTypeExtended)layer).embeddium$getChunkLayerId();
        if(id < 0) {
            throw new UnsupportedOperationException("Unsupported render type: " + layer);
        }
        SinkingVertexBuilder builder = vertexBuilderMap[id];
        if(builder == null) {
            builder = new SinkingVertexBuilder();
            vertexBuilderMap[id] = builder;
        }
        return builder;
    }

    public void reset() {
        for(SinkingVertexBuilder builder : vertexBuilderMap) {
            if(builder != null) {
                builder.reset();
            }
        }
        cullChecked = 0;
        cullValue = 0;
    }

    public void renderEmbeddium(me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext ctx,
                                PoseStack mStack,
                                RandomSource random) {
        this.currentContext = ctx;
        try {
            this.render(ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), mStack, null, true, random, ctx.seed(), OverlayTexture.NO_OVERLAY);
        } finally {
            this.currentContext = null;
        }
    }

    /**
     * Flush the rendered data to Sodium's chunk mesh builder.
     * @param buffers A pack of build buffers for render types
     * @param origin The origin of this block
     */
    public void flush(ChunkBuildBuffers buffers, Vector3fc origin) {
        for(int i = 0; i < vertexBuilderMap.length; i++) {
            var sinkingVertexBuilder = vertexBuilderMap[i];
            if(sinkingVertexBuilder == null || sinkingVertexBuilder.isEmpty()) {
                continue;
            }
            var material = DefaultMaterials.forRenderLayer(RenderType.chunkBufferLayers().get(i));
            var builder = buffers.get(material);
            sinkingVertexBuilder.flush(builder, material, origin);
        }
    }
}
