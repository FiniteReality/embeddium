package org.embeddedt.embeddium.render.chunk.light;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Matrix3f;
import com.mojang.math.Matrix4f;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.data.LightDataAccess;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraftforge.client.model.pipeline.VertexLighterFlat;
import net.minecraftforge.client.model.pipeline.VertexLighterSmoothAo;

/**
 * Implements an Embeddium-compatible frontend for the Forge light pipeline.
 */
public class ForgeLightPipeline implements LightPipeline {
    private final VertexLighterFlat forgeLighter;
    private final BlockAndTintGetter level;
    private final LightDataConsumer consumer = new LightDataConsumer();
    private final int[] mutableQuadVertexData = new int[32];
    private final BakedQuad mutableQuad = new BakedQuad(mutableQuadVertexData, -1, Direction.UP, null, false);
    private static final PoseStack.Pose EMPTY = new PoseStack.Pose(new Matrix4f(), new Matrix3f());

    private long cachedPos = Long.MIN_VALUE;

    public ForgeLightPipeline(LightDataAccess cache, VertexLighterFlat forgeLighter) {
        this.forgeLighter = forgeLighter;
        this.level = cache.getWorld();
    }

    public static ForgeLightPipeline smooth(LightDataAccess cache) {
        return new ForgeLightPipeline(cache, new VertexLighterSmoothAo(new BlockColors()));
    }

    public static ForgeLightPipeline flat(LightDataAccess cache) {
        return new ForgeLightPipeline(cache, new VertexLighterFlat(new BlockColors()));
    }

    private void computeLightData(BlockPos pos) {
        long key = pos.asLong();
        if(this.cachedPos != key) {
            forgeLighter.setBlockPos(pos);
            forgeLighter.setWorld(level);
            forgeLighter.setState(level.getBlockState(pos));
            this.cachedPos = key;
        }
    }

    private BakedQuad generateForgeQuad(ModelQuadView quad) {
        int[] vData = this.mutableQuadVertexData;
        for(int i = 0; i < 4; i++) {
            int vertexBase = i * ModelQuadUtil.VERTEX_SIZE;
            vData[vertexBase + ModelQuadUtil.POSITION_INDEX] = Float.floatToIntBits(quad.getX(i));
            vData[vertexBase + ModelQuadUtil.POSITION_INDEX + 1] = Float.floatToIntBits(quad.getY(i));
            vData[vertexBase + ModelQuadUtil.POSITION_INDEX + 2] = Float.floatToIntBits(quad.getZ(i));
            vData[vertexBase + ModelQuadUtil.NORMAL_INDEX] = quad.getForgeNormal(i);
            vData[vertexBase + ModelQuadUtil.LIGHT_INDEX] = quad.getLight(i);
        }
        return mutableQuad;
    }

    @Override
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
        this.computeLightData(pos);
        BakedQuad forgeQuad;
        if(quad instanceof BakedQuad) {
            forgeQuad = (BakedQuad)quad;
        } else {
            forgeQuad = generateForgeQuad(quad);
        }
        if(true) throw new UnsupportedOperationException("TODO");
        //forgeLighter.process(consumer, EMPTY, forgeQuad, OverlayTexture.NO_OVERLAY);
        System.arraycopy(consumer.lm, 0, out.lm, 0, 4);
        System.arraycopy(consumer.brightness, 0, out.br, 0, 4);
    }

    @Override
    public void reset() {
        this.cachedPos = Long.MIN_VALUE;
    }

    static class LightDataConsumer implements VertexConsumer {
        float[] brightness;
        int[] lm;

        // This should always override the exact overload called in QuadLighter#process. We use it to capture the
        // brightness and lightmap.
        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness, float r, float g, float b, int[] lm, int overlay, boolean colorize) {
            this.brightness = brightness;
            this.lm = lm;
        }

        @Override
        public VertexConsumer vertex(double pX, double pY, double pZ) {
            return this;
        }

        @Override
        public VertexConsumer color(int pRed, int pGreen, int pBlue, int pAlpha) {
            return this;
        }

        @Override
        public VertexConsumer uv(float pU, float pV) {
            return this;
        }

        @Override
        public VertexConsumer overlayCoords(int pU, int pV) {
            return this;
        }

        @Override
        public VertexConsumer uv2(int pU, int pV) {
            return this;
        }

        @Override
        public VertexConsumer normal(float pX, float pY, float pZ) {
            return this;
        }

        @Override
        public void endVertex() {

        }
    }
}
