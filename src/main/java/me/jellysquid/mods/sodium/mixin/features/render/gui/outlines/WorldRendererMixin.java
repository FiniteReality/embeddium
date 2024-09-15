package me.jellysquid.mods.sodium.mixin.features.render.gui.outlines;

import net.caffeinemc.mods.sodium.api.vertex.format.common.LineVertex;
import net.minecraft.client.renderer.LevelRenderer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LevelRenderer.class)
public class WorldRendererMixin {
    /**
     * @author JellySquid
     * @reason Use intrinsics where possible to speed up vertex writing
     */
    @Inject(method = "renderLineBox(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;DDDDDDFFFFFFF)V", at = @At("HEAD"), cancellable = true)
    private static void drawBoxFast(PoseStack matrices, VertexConsumer vertexConsumer, double x1, double y1, double z1,
                                    double x2, double y2, double z2, float red, float green, float blue, float alpha,
                                    float xAxisRed, float yAxisGreen, float zAxisBlue, CallbackInfo ci) {
        var writer = VertexBufferWriter.tryOf(vertexConsumer);

        if (writer == null)
            return;

        ci.cancel();

        Matrix4fExtended matrixExt = Matrix4fExtended.get(matrices.last().pose());

        float x1f = (float) x1;
        float y1f = (float) y1;
        float z1f = (float) z1;
        float x2f = (float) x2;
        float y2f = (float) y2;
        float z2f = (float) z2;

        int color = ColorABGR.pack(red, green, blue, alpha);

        float v1x = matrixExt.transformVecX(x1f, y1f, z1f);
        float v1y = matrixExt.transformVecY(x1f, y1f, z1f);
        float v1z = matrixExt.transformVecZ(x1f, y1f, z1f);

        float v2x = matrixExt.transformVecX(x2f, y1f, z1f);
        float v2y = matrixExt.transformVecY(x2f, y1f, z1f);
        float v2z = matrixExt.transformVecZ(x2f, y1f, z1f);

        float v3x = matrixExt.transformVecX(x1f, y2f, z1f);
        float v3y = matrixExt.transformVecY(x1f, y2f, z1f);
        float v3z = matrixExt.transformVecZ(x1f, y2f, z1f);

        float v4x = matrixExt.transformVecX(x1f, y1f, z2f);
        float v4y = matrixExt.transformVecY(x1f, y1f, z2f);
        float v4z = matrixExt.transformVecZ(x1f, y1f, z2f);

        float v5x = matrixExt.transformVecX(x2f, y2f, z1f);
        float v5y = matrixExt.transformVecY(x2f, y2f, z1f);
        float v5z = matrixExt.transformVecZ(x2f, y2f, z1f);

        float v6x = matrixExt.transformVecX(x1f, y2f, z2f);
        float v6y = matrixExt.transformVecY(x1f, y2f, z2f);
        float v6z = matrixExt.transformVecZ(x1f, y2f, z2f);

        float v7x = matrixExt.transformVecX(x2f, y1f, z2f);
        float v7y = matrixExt.transformVecY(x2f, y1f, z2f);
        float v7z = matrixExt.transformVecZ(x2f, y1f, z2f);

        float v8x = matrixExt.transformVecX(x2f, y2f, z2f);
        float v8y = matrixExt.transformVecY(x2f, y2f, z2f);
        float v8z = matrixExt.transformVecZ(x2f, y2f, z2f);

        writeLineVertex(writer, v1x, v1y, v1z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha));
        writeLineVertex(writer, v2x, v2y, v2z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha));
        writeLineVertex(writer, v1x, v1y, v1z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha));
        writeLineVertex(writer, v3x, v3y, v3z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha));
        writeLineVertex(writer, v1x, v1y, v1z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha));
        writeLineVertex(writer, v4x, v4y, v4z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha));
        writeLineVertex(writer, v2x, v2y, v2z, color);
        writeLineVertex(writer, v5x, v5y, v5z, color);
        writeLineVertex(writer, v5x, v5y, v5z, color);
        writeLineVertex(writer, v3x, v3y, v3z, color);
        writeLineVertex(writer, v3x, v3y, v3z, color);
        writeLineVertex(writer, v6x, v6y, v6z, color);
        writeLineVertex(writer, v6x, v6y, v6z, color);
        writeLineVertex(writer, v4x, v4y, v4z, color);
        writeLineVertex(writer, v4x, v4y, v4z, color);
        writeLineVertex(writer, v7x, v7y, v7z, color);
        writeLineVertex(writer, v7x, v7y, v7z, color);
        writeLineVertex(writer, v2x, v2y, v2z, color);
        writeLineVertex(writer, v6x, v6y, v6z, color);
        writeLineVertex(writer, v8x, v8y, v8z, color);
        writeLineVertex(writer, v7x, v7y, v7z, color);
        writeLineVertex(writer, v8x, v8y, v8z, color);
        writeLineVertex(writer, v5x, v5y, v5z, color);
        writeLineVertex(writer, v8x, v8y, v8z, color);
    }

    @Unique
    private static void writeLineVertex(VertexBufferWriter writer, float x, float y, float z, int color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(LineVertex.STRIDE);
            LineVertex.put(buffer, x, y, z, color);
            writer.push(stack, buffer, 1, LineVertex.FORMAT);
        }
    }

}
