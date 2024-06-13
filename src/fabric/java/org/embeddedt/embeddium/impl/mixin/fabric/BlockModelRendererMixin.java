package org.embeddedt.embeddium.impl.mixin.fabric;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.ModelBlockRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ModelBlockRenderer.class, priority = 100)
public abstract class BlockModelRendererMixin {
    @Shadow
    public abstract void renderModel(PoseStack.Pose pose, VertexConsumer consumer, @Nullable BlockState state, BakedModel model, float red, float green, float blue, int packedLight, int packedOverlay);

    public void renderModel(PoseStack.Pose pose, VertexConsumer consumer, @Nullable BlockState state, BakedModel model, float red, float green, float blue, int packedLight, int packedOverlay, ModelData data, RenderType type) {
        renderModel(pose, consumer, state, model, red, green, blue, packedLight, packedOverlay);
    }
}
