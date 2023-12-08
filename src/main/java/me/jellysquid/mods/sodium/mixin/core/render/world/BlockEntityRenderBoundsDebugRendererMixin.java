package me.jellysquid.mods.sodium.mixin.core.render.world;

import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Vec3d;
import net.neoforged.neoforge.client.BlockEntityRenderBoundsDebugRenderer;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockEntityRenderBoundsDebugRenderer.class)
public abstract class BlockEntityRenderBoundsDebugRendererMixin {
    @Shadow
    private static void drawRenderBoundingBox(MatrixStack poseStack, VertexConsumer consumer, Vec3d camera, BlockEntity be) {
        throw new AssertionError();
    }

    @Inject(method = "onRenderLevelStage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/render/WorldRenderer;getGlobalBlockEntities()Ljava/util/Set;"))
    private static void embeddium$renderBEBounds(RenderLevelStageEvent event, CallbackInfo ci) {
        MatrixStack poseStack = event.getPoseStack();
        Vec3d camera = event.getCamera().getPos();
        VertexConsumer consumer = MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers().getBuffer(RenderLayer.getLines());

        SodiumWorldRenderer.instance().forEachVisibleBlockEntity(be -> drawRenderBoundingBox(poseStack, consumer, camera, be));
    }
}
