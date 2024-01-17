package me.jellysquid.mods.sodium.mixin.core.render.world;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.SodiumWorldRenderer;
import me.jellysquid.mods.sodium.client.world.WorldRendererExtended;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.Vec3;
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
    private static void drawRenderBoundingBox(PoseStack poseStack, VertexConsumer consumer, Vec3 camera, BlockEntity be) {
        throw new AssertionError();
    }

    @Inject(method = "onRenderLevelStage", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getGlobalBlockEntities()Ljava/util/Set;"))
    private static void embeddium$renderBEBounds(RenderLevelStageEvent event, CallbackInfo ci) {
        PoseStack poseStack = event.getPoseStack();
        Vec3 camera = event.getCamera().getPosition();
        VertexConsumer consumer = Minecraft.getInstance().renderBuffers().bufferSource().getBuffer(RenderType.lines());

        SodiumWorldRenderer.instance().forEachVisibleBlockEntity(be -> drawRenderBoundingBox(poseStack, consumer, camera, be));
    }
}
