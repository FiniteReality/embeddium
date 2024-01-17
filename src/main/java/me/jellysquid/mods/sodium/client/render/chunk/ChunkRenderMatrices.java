package me.jellysquid.mods.sodium.client.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.util.math.JomlHelper;
import repack.joml.Matrix4f;

public record ChunkRenderMatrices(Matrix4f projection, Matrix4f modelView) {
    public static ChunkRenderMatrices from(PoseStack stack) {
        PoseStack.Pose entry = stack.last();
        return new ChunkRenderMatrices(JomlHelper.copy(RenderSystem.getProjectionMatrix()), JomlHelper.copy(entry.pose()));
    }
}
