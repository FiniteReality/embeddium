package me.jellysquid.mods.sodium.mixin.features.render.entity.shadows;

import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.math.MatrixHelper;
import org.embeddedt.embeddium.api.math.Matrix3fExtended;
import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {
    @Unique
    private static final int SHADOW_COLOR = ColorABGR.pack(1.0f, 1.0f, 1.0f);

    /**
     * @author JellySquid
     * @reason Reduce vertex assembly overhead for shadow rendering
     */
    @Inject(method = "renderBlockShadow", at = @At("HEAD"), cancellable = true)
    private static void renderShadowPartFast(PoseStack.Pose entry, VertexConsumer vertices, LevelReader world, BlockPos pos, double x, double y, double z, float radius, float opacity, CallbackInfo ci) {
        var writer = VertexBufferWriter.tryOf(vertices);

        if (writer == null)
            return;

        ci.cancel();

        BlockPos blockPos = pos.below();
        BlockState blockState = world.getBlockState(blockPos);

        if (blockState.getRenderShape() == RenderShape.INVISIBLE || !blockState.isCollisionShapeFullBlock(world, blockPos)) {
            return;
        }

        var light = world.getMaxLocalRawBrightness(pos);

        if (light <= 3) {
            return;
        }

        VoxelShape voxelShape = blockState.getShape(world, blockPos);

        if (voxelShape.isEmpty()) {
            return;
        }

        float brightness = world.dimensionType().brightness(light);
        float alpha = (float) (((double) opacity - ((y - (double) pos.getY()) / 2.0)) * 0.5 * (double) brightness);

        if (alpha >= 0.0F) {
            if (alpha > 1.0F) {
                alpha = 1.0F;
            }

            AABB box = voxelShape.bounds();

            float minX = (float) ((pos.getX() + box.minX) - x);
            float maxX = (float) ((pos.getX() + box.maxX) - x);

            float minY = (float) ((pos.getY() + box.minY) - y);

            float minZ = (float) ((pos.getZ() + box.minZ) - z);
            float maxZ = (float) ((pos.getZ() + box.maxZ) - z);

            renderShadowPart(entry, writer, radius, alpha, minX, maxX, minY, minZ, maxZ);
        }
    }

    /**
     * @deprecated don't call, but just in case...
     */
    @Deprecated
    private static void renderShadowPart(PoseStack.Pose matrices, VertexConsumer consumer, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        renderShadowPart(matrices, VertexBufferWriter.of(consumer), radius, alpha, minX, maxX, minY, minZ, maxZ);
    }

    @Unique
    private static void renderShadowPart(PoseStack.Pose matrices, VertexBufferWriter writer, float radius, float alpha, float minX, float maxX, float minY, float minZ, float maxZ) {
        float size = 0.5F * (1.0F / radius);

        float u1 = (-minX * size) + 0.5F;
        float u2 = (-maxX * size) + 0.5F;

        float v1 = (-minZ * size) + 0.5F;
        float v2 = (-maxZ * size) + 0.5F;

        var matNormal = matrices.normal();
        var matPosition = Matrix4fExtended.get(matrices.pose());

        var color = ColorABGR.withAlpha(SHADOW_COLOR, alpha);
        var normal = Matrix3fExtended.get(matNormal).computeNormal(Direction.UP);

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            writeShadowVertex(ptr, matPosition, minX, minY, minZ, u1, v1, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, minX, minY, maxZ, u1, v2, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, maxZ, u2, v2, color, normal);
            ptr += ModelVertex.STRIDE;

            writeShadowVertex(ptr, matPosition, maxX, minY, minZ, u2, v1, color, normal);
            ptr += ModelVertex.STRIDE;

            writer
                    .push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }

    private static final int FULL_BRIGHT = LightTexture.pack(15, 15);

    @Unique
    private static void writeShadowVertex(long ptr, Matrix4fExtended matPosition, float x, float y, float z, float u, float v, int color, int normal) {
        // The transformed position vector
        float xt = matPosition.transformVecX(x, y, z);
        float yt = matPosition.transformVecY(x, y, z);
        float zt = matPosition.transformVecZ(x, y, z);

        ModelVertex.write(ptr, xt, yt, zt, color, u, v, FULL_BRIGHT, OverlayTexture.NO_OVERLAY, normal);
    }
}
