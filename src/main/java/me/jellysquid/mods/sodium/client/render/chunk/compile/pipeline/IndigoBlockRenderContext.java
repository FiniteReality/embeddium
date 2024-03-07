package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.util.Map;

/**
 * Adaptation of Indigo's {@link BlockRenderContext} that delegates back to the Sodium renderer.
 */
public class IndigoBlockRenderContext extends BlockRenderContext {
    private final Map<RenderType, SinkingVertexBuilder> vertexBuilderMap = new Object2ObjectOpenHashMap<>();

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
        return vertexBuilderMap.computeIfAbsent(layer, k -> new SinkingVertexBuilder());
    }

    public void reset() {
        vertexBuilderMap.values().forEach(SinkingVertexBuilder::reset);
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
        vertexBuilderMap.forEach((renderType, sinkingVertexBuilder) -> {
            if(sinkingVertexBuilder.isEmpty()) {
                return;
            }
            var material = DefaultMaterials.forRenderLayer(renderType);
            var builder = buffers.get(material);
            sinkingVertexBuilder.flush(builder, material, origin);
        });
    }
}
