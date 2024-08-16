package org.embeddedt.embeddium.impl.render.chunk.light;

import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.data.LightDataAccess;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.neoforged.neoforge.client.model.IQuadTransformer;
import net.neoforged.neoforge.client.model.lighting.FlatQuadLighter;
import net.neoforged.neoforge.client.model.lighting.QuadLighter;
import net.neoforged.neoforge.client.model.lighting.SmoothQuadLighter;
import net.neoforged.neoforge.client.textures.UnitTextureAtlasSprite;

/**
 * Implements an Embeddium-compatible frontend for the Forge light pipeline.
 */
public class ForgeLightPipeline implements LightPipeline {
    private final QuadLighter forgeLighter;
    private final BlockAndTintGetter level;
    private final int[] mutableQuadVertexData = new int[32];
    private final BakedQuad mutableQuadWithoutShade = new BakedQuad(mutableQuadVertexData, -1, Direction.UP, UnitTextureAtlasSprite.INSTANCE, false, 0);
    private final BakedQuad mutableQuadWithShade = new BakedQuad(mutableQuadVertexData, -1, Direction.UP, UnitTextureAtlasSprite.INSTANCE, true, 0);

    private long cachedPos = Long.MIN_VALUE;

    public ForgeLightPipeline(LightDataAccess cache, QuadLighter forgeLighter) {
        this.forgeLighter = forgeLighter;
        this.level = cache.getWorld();
    }

    public static ForgeLightPipeline smooth(LightDataAccess cache) {
        return new ForgeLightPipeline(cache, new SmoothQuadLighter(new BlockColors()));
    }

    public static ForgeLightPipeline flat(LightDataAccess cache) {
        return new ForgeLightPipeline(cache, new FlatQuadLighter(new BlockColors()));
    }

    private void computeLightData(BlockPos pos) {
        long key = pos.asLong();
        if(this.cachedPos != key) {
            forgeLighter.setup(level, pos, level.getBlockState(pos));
            this.cachedPos = key;
        }
    }

    private BakedQuad generateForgeQuad(ModelQuadView quad, boolean hasShade) {
        int[] vData = this.mutableQuadVertexData;
        for(int i = 0; i < 4; i++) {
            int vertexBase = i * IQuadTransformer.STRIDE;
            vData[vertexBase + IQuadTransformer.POSITION] = Float.floatToIntBits(quad.getX(i));
            vData[vertexBase + IQuadTransformer.POSITION + 1] = Float.floatToIntBits(quad.getY(i));
            vData[vertexBase + IQuadTransformer.POSITION + 2] = Float.floatToIntBits(quad.getZ(i));
            vData[vertexBase + IQuadTransformer.NORMAL] = quad.getForgeNormal(i);
            // Do not tell Forge about the packed light, so that it doesn't use it in the lightmap calculation
            vData[vertexBase + IQuadTransformer.UV2] = 0;
        }
        return hasShade ? this.mutableQuadWithShade : this.mutableQuadWithoutShade;
    }

    @Override
    public void calculate(ModelQuadView quad, BlockPos pos, QuadLightData out, Direction cullFace, Direction lightFace, boolean shade) {
        this.computeLightData(pos);
        BakedQuad forgeQuad;
        if(quad instanceof BakedQuad) {
            forgeQuad = generateForgeQuad(quad, ((BakedQuad)quad).isShade());
        } else {
            forgeQuad = generateForgeQuad(quad, false);
        }
        forgeLighter.computeLightingForQuad(forgeQuad);
        System.arraycopy(forgeLighter.getComputedLightmap(), 0, out.lm, 0, 4);
        System.arraycopy(forgeLighter.getComputedBrightness(), 0, out.br, 0, 4);
    }

    @Override
    public void reset() {
        this.cachedPos = Long.MIN_VALUE;
    }
}
