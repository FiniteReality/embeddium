package me.jellysquid.mods.sodium.mixin.features.block;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.vertex.VanillaVertexTypes;
import me.jellysquid.mods.sodium.client.model.vertex.VertexDrain;
import me.jellysquid.mods.sodium.client.model.vertex.formats.quad.QuadVertexSink;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;
import net.minecraftforge.client.model.data.ModelData;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;

@Mixin(ModelBlockRenderer.class)
public class MixinBlockModelRenderer {
	private final XoroshiroRandomSource random = new XoroshiroRandomSource(42L);

    /*@Inject(method = "render(Lnet/minecraft/world/BlockRenderView;Lnet/minecraft/client/render/model/BakedModel;Lnet/minecraft/block/BlockState;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumer;ZLnet/minecraft/util/math/random/Random;JI)Z", at = @At("HEAD"), cancellable = true)
    private void preRenderBlockInWorld(BlockRenderView world, BakedModel model, BlockState state, BlockPos pos, MatrixStack matrixStack, VertexConsumer consumer, boolean cull, Random rand, long seed, int int_1, CallbackInfoReturnable<Boolean> cir) {
//        GlobalRenderContext renderer = GlobalRenderContext.getInstance(world);
//        BlockRenderer blockRenderer = renderer.getBlockRenderer();
//
//        boolean ret = blockRenderer.renderModel(world, state, pos, model, new FallbackChunkModelBuffers(consumer, matrixStack), cull, seed);
//
//        cir.setReturnValue(ret);
    }*/

    /**
     * @reason Use optimized vertex writer intrinsics, avoid allocations
     * @author JellySquid
     */
    @Overwrite(remap = false)
    public void renderModel(PoseStack.Pose entry, VertexConsumer vertexConsumer, BlockState blockState, BakedModel bakedModel, float red, float green, float blue, int light, int overlay, ModelData data, RenderType layer) {
        QuadVertexSink drain = VertexDrain.of(vertexConsumer)
                .createSink(VanillaVertexTypes.QUADS);
        XoroshiroRandomSource random = this.random;

        // Clamp color ranges
        red = Mth.clamp(red, 0.0F, 1.0F);
        green = Mth.clamp(green, 0.0F, 1.0F);
        blue = Mth.clamp(blue, 0.0F, 1.0F);

        int defaultColor = ColorABGR.pack(red, green, blue, 1.0F);

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
        	random.setSeed(42L);
            List<BakedQuad> quads = bakedModel.getQuads(blockState, direction, random, data, layer);

            if (!quads.isEmpty()) {
                renderQuad(entry, drain, defaultColor, quads, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = bakedModel.getQuads(blockState, null, random, data, layer);

        if (!quads.isEmpty()) {
            renderQuad(entry, drain, defaultColor, quads, light, overlay);
        }

        drain.flush();
    }

    private static void renderQuad(PoseStack.Pose entry, QuadVertexSink drain, int defaultColor, List<BakedQuad> list, int light, int overlay) {
        if (list.isEmpty()) {
            return;
        }

        drain.ensureCapacity(list.size() * 4);

        for (BakedQuad bakedQuad : list) {
            int color = bakedQuad.isTinted() ? defaultColor : 0xFFFFFFFF;

            ModelQuadView quad = ((ModelQuadView) bakedQuad);

            for (int i = 0; i < 4; i++) {
                drain.writeQuad(entry, quad.getX(i), quad.getY(i), quad.getZ(i), color, quad.getTexU(i), quad.getTexV(i),
                        light, overlay, ModelQuadUtil.getFacingNormal(bakedQuad.getDirection(), quad.getNormal(i)));
            }

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
