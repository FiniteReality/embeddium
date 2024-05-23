package org.embeddedt.embeddium.render.chunk;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

public record ChunkRenderMatrices(Matrix4fc projection, Matrix4fc modelView) {
    public static ChunkRenderMatrices from(Matrix4f pose) {
        return new ChunkRenderMatrices(new Matrix4f(RenderSystem.getProjectionMatrix()), new Matrix4f(pose));
    }
}
