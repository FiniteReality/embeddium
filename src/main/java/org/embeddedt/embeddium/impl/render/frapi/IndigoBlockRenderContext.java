package org.embeddedt.embeddium.impl.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.impl.client.indigo.renderer.aocalc.AoCalculator;
import net.fabricmc.fabric.impl.client.indigo.renderer.mesh.MutableQuadViewImpl;
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
import net.neoforged.neoforge.client.model.data.ModelData;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelVertexConsumer;
import org.embeddedt.embeddium.impl.render.chunk.compile.pipeline.BlockOcclusionCache;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.DefaultMaterials;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * Adaptation of Indigo's {@link BlockRenderContext} that delegates back to the Sodium renderer.
 */
public class IndigoBlockRenderContext extends BlockRenderContext implements FRAPIRenderHandler {
    private org.embeddedt.embeddium.api.render.chunk.BlockRenderContext currentContext;
    private ChunkBuildBuffers currentBuffers;
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
    protected VertexConsumer getVertexConsumer(RenderType layer) {
        var material = DefaultMaterials.forRenderLayer(layer);
        var consumer = currentBuffers.get(material).asVertexConsumer(material);
        consumer.embeddium$setOffset(currentContext.origin());
        return consumer;
    }

    @Override
    protected void bufferQuad(MutableQuadViewImpl quad, VertexConsumer vertexConsumer) {
        super.bufferQuad(quad, vertexConsumer);
        if(vertexConsumer instanceof ChunkModelVertexConsumer modelConsumer) {
            modelConsumer.close();
        }
    }

    public void reset() {
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

    public void renderEmbeddium(org.embeddedt.embeddium.api.render.chunk.BlockRenderContext ctx,
                                ChunkBuildBuffers buffers,
                                PoseStack mStack,
                                RandomSource random) {
        this.currentContext = ctx;
        this.currentBuffers = buffers;
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
            this.currentBuffers = null;
        }
    }
}
