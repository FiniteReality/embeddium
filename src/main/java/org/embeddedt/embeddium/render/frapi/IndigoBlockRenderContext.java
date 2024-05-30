package org.embeddedt.embeddium.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockOcclusionCache;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext;
import net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderInfo;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3fc;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.function.Supplier;

/**
 * Adaptation of Indigo's {@link BlockRenderContext} that delegates back to the Sodium renderer.
 */
public class IndigoBlockRenderContext extends BlockRenderContext implements FRAPIRenderHandler {
    private final SinkingVertexBuilder[] vertexBuilderMap = new SinkingVertexBuilder[RenderType.chunkBufferLayers().size()];

    private me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext currentContext;
    private final BlockOcclusionCache occlusionCache;
    private final LightDataAccess lightDataAccess;

    private int cullChecked, cullValue;

    private static final MethodHandle FABRIC_RENDER_HANDLE, FORGIFIED_RENDER_HANDLE;

    static {
        MethodHandle fabricHandle = null, forgeHandle = null;
        ReflectiveOperationException forgeException = null, fabricException = null;
        try {
            fabricHandle = MethodHandles.lookup().findVirtual(BlockRenderContext.class, "render", MethodType.methodType(void.class, BlockAndTintGetter.class, BakedModel.class, BlockState.class, BlockPos.class, PoseStack.class, VertexConsumer.class, boolean.class, RandomSource.class, long.class, int.class));
        } catch(ReflectiveOperationException e) {
            fabricException = e;
        }
        try {
            forgeHandle = MethodHandles.lookup().findVirtual(BlockRenderContext.class, "render", MethodType.methodType(void.class, BlockAndTintGetter.class, BakedModel.class, BlockState.class, BlockPos.class, PoseStack.class, VertexConsumer.class, boolean.class, RandomSource.class, long.class, int.class, ModelData.class, RenderType.class));
        } catch(ReflectiveOperationException e) {
            forgeException = e;
        }
        if(fabricHandle == null && forgeHandle == null) {
            var ex = new IllegalStateException("Failed to find render method on BlockRenderContext.");
            if(fabricException != null) {
                ex.addSuppressed(fabricException);
            }
            if(forgeException != null) {
                ex.addSuppressed(forgeException);
            }
            throw ex;
        }
        FABRIC_RENDER_HANDLE = fabricHandle;
        FORGIFIED_RENDER_HANDLE = forgeHandle;
    }

    public IndigoBlockRenderContext(BlockOcclusionCache occlusionCache, LightDataAccess lightDataAccess) {
        this.occlusionCache = occlusionCache;
        this.lightDataAccess = lightDataAccess;
    }

    @Override
    protected AoCalculator createAoCalc(BlockRenderInfo blockInfo) {
        return new AoCalculator(blockInfo) {
            @Override
            public int light(BlockPos pos, BlockState state) {
                int data = lightDataAccess.get(pos);
                return LightDataAccess.getLightmap(data);
            }

            @Override
            public float ao(BlockPos pos, BlockState state) {
                return LightDataAccess.unpackAO(lightDataAccess.get(pos));
            }
        };
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
        int id = layer.getChunkLayerId();
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

    private RuntimeException processException(Throwable e) {
        if(e instanceof RuntimeException) {
            return (RuntimeException)e;
        } else {
            return new IllegalStateException("Unexpected throwable", e);
        }
    }

    public void renderEmbeddium(me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderContext ctx,
                                PoseStack mStack,
                                RandomSource random) {
        this.currentContext = ctx;
        // We unfortunately have no choice but to push a pose here since FRAPI now mutates the given stack
        mStack.pushPose();
        try {
            if(FABRIC_RENDER_HANDLE != null) {
                FABRIC_RENDER_HANDLE.invokeExact((BlockRenderContext)this, (BlockAndTintGetter)ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), mStack, (VertexConsumer)null, true, random, ctx.seed(), OverlayTexture.NO_OVERLAY);
            } else if(FORGIFIED_RENDER_HANDLE != null) {
                FORGIFIED_RENDER_HANDLE.invokeExact((BlockRenderContext)this, (BlockAndTintGetter)ctx.localSlice(), ctx.model(), ctx.state(), ctx.pos(), mStack, (VertexConsumer)null, true, random, ctx.seed(), OverlayTexture.NO_OVERLAY, ctx.modelData(), ctx.renderLayer());
            }
        } catch(Throwable e) {
            throw processException(e);
        } finally {
            mStack.popPose();
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
