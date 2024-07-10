package org.embeddedt.embeddium.impl.mixin.features.render.model.item;

import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.render.immediate.model.BakedModelEncoder;
import org.embeddedt.embeddium.api.render.texture.SpriteUtil;
import org.embeddedt.embeddium.impl.render.vertex.VertexConsumerUtils;
import org.embeddedt.embeddium.impl.model.color.interop.ItemColorsExtended;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.SingleThreadedRandomSource;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;

@Mixin(ItemRenderer.class)
public class ItemRendererMixin {
    @Unique
    private final RandomSource random = new SingleThreadedRandomSource(42L);

    @Shadow
    @Final
    private ItemColors itemColors;

    /**
     * @reason Avoid allocations
     * @author JellySquid
     */
    @Inject(method = "renderModelLists", at = @At("HEAD"), cancellable = true)
    private void renderModelFast(BakedModel model, ItemStack itemStack, int light, int overlay, PoseStack matrixStack, VertexConsumer vertexConsumer, CallbackInfo ci) {
        var writer = VertexConsumerUtils.convertOrLog(vertexConsumer);

        if (writer == null) {
            return;
        }

        ci.cancel();

        RandomSource random = this.random;
        PoseStack.Pose matrices = matrixStack.last();

        ItemColor colorProvider = null;

        if (!itemStack.isEmpty()) {
            colorProvider = ((ItemColorsExtended) this.itemColors).sodium$getColorProvider(itemStack);
        }

        for (Direction direction : DirectionUtil.ALL_DIRECTIONS) {
            random.setSeed(42L);
            List<BakedQuad> quads = model.getQuads(null, direction, random);

            if (!quads.isEmpty()) {
                this.renderBakedItemQuads(matrices, writer, quads, itemStack, colorProvider, light, overlay);
            }
        }

        random.setSeed(42L);
        List<BakedQuad> quads = model.getQuads(null, null, random);

        if (!quads.isEmpty()) {
            this.renderBakedItemQuads(matrices, writer, quads, itemStack, colorProvider, light, overlay);
        }
    }

    @Unique
    @SuppressWarnings("ForLoopReplaceableByForEach")
    private void renderBakedItemQuads(PoseStack.Pose matrices, VertexBufferWriter writer, List<BakedQuad> quads, ItemStack itemStack, ItemColor colorProvider, int light, int overlay) {
        for (int i = 0; i < quads.size(); i++) {
            BakedQuad bakedQuad = quads.get(i);

            if (bakedQuad.getVertices().length < 32) {
                continue; // ignore bad quads
            }

            BakedQuadView quad = (BakedQuadView) bakedQuad;

            int color = 0xFFFFFFFF;

            if (colorProvider != null && quad.hasColor()) {
                color = ColorARGB.toABGR(colorProvider.getColor(itemStack, quad.getColorIndex()));
            }

            BakedModelEncoder.writeQuadVertices(writer, matrices, quad, color, light, overlay, true);

            SpriteUtil.markSpriteActive(quad.getSprite());
        }
    }
}
