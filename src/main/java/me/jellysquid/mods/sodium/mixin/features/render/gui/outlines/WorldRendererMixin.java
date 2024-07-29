package me.jellysquid.mods.sodium.mixin.features.render.gui.outlines;

import net.caffeinemc.mods.sodium.api.vertex.format.common.LineVertex;
import net.minecraft.client.renderer.LevelRenderer;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.math.Matrix3fExtended;
import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
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

        Matrix4fExtended position = Matrix4fExtended.get(matrices.last().pose());
        Matrix3fExtended normal = Matrix3fExtended.get(matrices.last().normal());

        float x1f = (float) x1;
        float y1f = (float) y1;
        float z1f = (float) z1;
        float x2f = (float) x2;
        float y2f = (float) y2;
        float z2f = (float) z2;

        int color = ColorABGR.pack(red, green, blue, alpha);

        float v1x = Math.fma(position.getA00(), x1f, Math.fma(position.getA10(), y1f, Math.fma(position.getA20(), z1f, position.getA30())));
        float v1y = Math.fma(position.getA01(), x1f, Math.fma(position.getA11(), y1f, Math.fma(position.getA21(), z1f, position.getA31())));
        float v1z = Math.fma(position.getA02(), x1f, Math.fma(position.getA12(), y1f, Math.fma(position.getA22(), z1f, position.getA32())));

        float v2x = Math.fma(position.getA00(), x2f, Math.fma(position.getA10(), y1f, Math.fma(position.getA20(), z1f, position.getA30())));
        float v2y = Math.fma(position.getA01(), x2f, Math.fma(position.getA11(), y1f, Math.fma(position.getA21(), z1f, position.getA31())));
        float v2z = Math.fma(position.getA02(), x2f, Math.fma(position.getA12(), y1f, Math.fma(position.getA22(), z1f, position.getA32())));

        float v3x = Math.fma(position.getA00(), x1f, Math.fma(position.getA10(), y2f, Math.fma(position.getA20(), z1f, position.getA30())));
        float v3y = Math.fma(position.getA01(), x1f, Math.fma(position.getA11(), y2f, Math.fma(position.getA21(), z1f, position.getA31())));
        float v3z = Math.fma(position.getA02(), x1f, Math.fma(position.getA12(), y2f, Math.fma(position.getA22(), z1f, position.getA32())));

        float v4x = Math.fma(position.getA00(), x1f, Math.fma(position.getA10(), y1f, Math.fma(position.getA20(), z2f, position.getA30())));
        float v4y = Math.fma(position.getA01(), x1f, Math.fma(position.getA11(), y1f, Math.fma(position.getA21(), z2f, position.getA31())));
        float v4z = Math.fma(position.getA02(), x1f, Math.fma(position.getA12(), y1f, Math.fma(position.getA22(), z2f, position.getA32())));

        float v5x = Math.fma(position.getA00(), x2f, Math.fma(position.getA10(), y2f, Math.fma(position.getA20(), z1f, position.getA30())));
        float v5y = Math.fma(position.getA01(), x2f, Math.fma(position.getA11(), y2f, Math.fma(position.getA21(), z1f, position.getA31())));
        float v5z = Math.fma(position.getA02(), x2f, Math.fma(position.getA12(), y2f, Math.fma(position.getA22(), z1f, position.getA32())));

        float v6x = Math.fma(position.getA00(), x1f, Math.fma(position.getA10(), y2f, Math.fma(position.getA20(), z2f, position.getA30())));
        float v6y = Math.fma(position.getA01(), x1f, Math.fma(position.getA11(), y2f, Math.fma(position.getA21(), z2f, position.getA31())));
        float v6z = Math.fma(position.getA02(), x1f, Math.fma(position.getA12(), y2f, Math.fma(position.getA22(), z2f, position.getA32())));

        float v7x = Math.fma(position.getA00(), x2f, Math.fma(position.getA10(), y1f, Math.fma(position.getA20(), z2f, position.getA30())));
        float v7y = Math.fma(position.getA01(), x2f, Math.fma(position.getA11(), y1f, Math.fma(position.getA21(), z2f, position.getA31())));
        float v7z = Math.fma(position.getA02(), x2f, Math.fma(position.getA12(), y1f, Math.fma(position.getA22(), z2f, position.getA32())));

        float v8x = Math.fma(position.getA00(), x2f, Math.fma(position.getA10(), y2f, Math.fma(position.getA20(), z2f, position.getA30())));
        float v8y = Math.fma(position.getA01(), x2f, Math.fma(position.getA11(), y2f, Math.fma(position.getA21(), z2f, position.getA31())));
        float v8z = Math.fma(position.getA02(), x2f, Math.fma(position.getA12(), y2f, Math.fma(position.getA22(), z2f, position.getA32())));

        writeLineVertices(writer, v1x, v1y, v1z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), NormI8.pack(normal.getA00(), normal.getA01(), normal.getA02()));
        writeLineVertices(writer, v2x, v2y, v2z, ColorABGR.pack(red, yAxisGreen, zAxisBlue, alpha), NormI8.pack(normal.getA00(), normal.getA01(), normal.getA02()));
        writeLineVertices(writer, v1x, v1y, v1z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), NormI8.pack(normal.getA10(), normal.getA11(), normal.getA12()));
        writeLineVertices(writer, v3x, v3y, v3z, ColorABGR.pack(xAxisRed, green, zAxisBlue, alpha), NormI8.pack(normal.getA10(), normal.getA11(), normal.getA12()));
        writeLineVertices(writer, v1x, v1y, v1z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), NormI8.pack(normal.getA20(), normal.getA21(), normal.getA22()));
        writeLineVertices(writer, v4x, v4y, v4z, ColorABGR.pack(xAxisRed, yAxisGreen, blue, alpha), NormI8.pack(normal.getA20(), normal.getA21(), normal.getA22()));
        writeLineVertices(writer, v2x, v2y, v2z, color, NormI8.pack(normal.getA10(), normal.getA11(), normal.getA12()));
        writeLineVertices(writer, v5x, v5y, v5z, color, NormI8.pack(normal.getA10(), normal.getA11(), normal.getA12()));
        writeLineVertices(writer, v5x, v5y, v5z, color, NormI8.pack(-normal.getA00(), -normal.getA01(), -normal.getA02()));
        writeLineVertices(writer, v3x, v3y, v3z, color, NormI8.pack(-normal.getA00(), -normal.getA01(), -normal.getA02()));
        writeLineVertices(writer, v3x, v3y, v3z, color, NormI8.pack(normal.getA20(), normal.getA21(), normal.getA22()));
        writeLineVertices(writer, v6x, v6y, v6z, color, NormI8.pack(normal.getA20(), normal.getA21(), normal.getA22()));
        writeLineVertices(writer, v6x, v6y, v6z, color, NormI8.pack(-normal.getA10(), -normal.getA11(), -normal.getA12()));
        writeLineVertices(writer, v4x, v4y, v4z, color, NormI8.pack(-normal.getA10(), -normal.getA11(), -normal.getA12()));
        writeLineVertices(writer, v4x, v4y, v4z, color, NormI8.pack(normal.getA00(), normal.getA01(), normal.getA02()));
        writeLineVertices(writer, v7x, v7y, v7z, color, NormI8.pack(normal.getA00(), normal.getA01(), normal.getA02()));
        writeLineVertices(writer, v7x, v7y, v7z, color, NormI8.pack(-normal.getA20(), -normal.getA21(), -normal.getA22()));
        writeLineVertices(writer, v2x, v2y, v2z, color, NormI8.pack(-normal.getA20(), -normal.getA21(), -normal.getA22()));
        writeLineVertices(writer, v6x, v6y, v6z, color, NormI8.pack(normal.getA00(), normal.getA01(), normal.getA02()));
        writeLineVertices(writer, v8x, v8y, v8z, color, NormI8.pack(normal.getA00(), normal.getA01(), normal.getA02()));
        writeLineVertices(writer, v7x, v7y, v7z, color, NormI8.pack(normal.getA10(), normal.getA11(), normal.getA12()));
        writeLineVertices(writer, v8x, v8y, v8z, color, NormI8.pack(normal.getA10(), normal.getA11(), normal.getA12()));
        writeLineVertices(writer, v5x, v5y, v5z, color, NormI8.pack(normal.getA20(), normal.getA21(), normal.getA22()));
        writeLineVertices(writer, v8x, v8y, v8z, color, NormI8.pack(normal.getA20(), normal.getA21(), normal.getA22()));
    }

    @Unique
    private static void writeLineVertices(VertexBufferWriter writer, float x, float y, float z, int color, int normal) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(2 * LineVertex.STRIDE);
            long ptr = buffer;

            for (int i = 0; i < 2; i++) {
                LineVertex.put(ptr, x, y, z, color);
                ptr += LineVertex.STRIDE;
            }

            writer.push(stack, buffer, 2, LineVertex.FORMAT);
        }

    }

}
