package me.jellysquid.mods.sodium.client.render.immediate.model;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.util.ModelQuadUtil;
import net.caffeinemc.mods.sodium.api.util.ColorABGR;
import net.caffeinemc.mods.sodium.api.util.ColorU8;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.caffeinemc.mods.sodium.api.vertex.buffer.VertexBufferWriter;
import net.caffeinemc.mods.sodium.api.vertex.format.common.ModelVertex;
import org.embeddedt.embeddium.api.math.Matrix3fExtended;
import org.embeddedt.embeddium.api.math.Matrix4fExtended;
import org.lwjgl.system.MemoryStack;

public class BakedModelEncoder {
    /**
     * Use the packed normal and transform it if set, otherwise use the precalculated and multiplied normal.
     */
    private static int mergeNormalAndMult(int packed, int calc, Matrix3fExtended matNormal) {
        if((packed & 0xFFFFFF) == 0)
            return calc;
        var x = NormI8.unpackX(packed);
        var y = NormI8.unpackY(packed);
        var z = NormI8.unpackZ(packed);
        var tX = matNormal.transformVecX(x, y, z);
        var tY = matNormal.transformVecY(x, y, z);
        var tZ = matNormal.transformVecZ(x, y, z);
        return NormI8.pack(tX, tY, tZ);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int color, int light, int overlay) {
        Matrix3fExtended matNormal = Matrix3fExtended.get(matrices.normal());
        Matrix4fExtended matPosition = Matrix4fExtended.get(matrices.pose());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            // The packed transformed normal vector
            var normal = matNormal.transformNormal(quad.getLightFace());

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = matPosition.transformVecX(x, y, z);
                float yt = matPosition.transformVecY(x, y, z);
                float zt = matPosition.transformVecZ(x, y, z);

                ModelVertex.write(ptr, xt, yt, zt, multARGBInts(quad.getColor(i), color), quad.getTexU(i), quad.getTexV(i), overlay, ModelQuadUtil.mergeBakedLight(quad.getLight(i), light), mergeNormalAndMult(quad.getForgeNormal(i), normal, matNormal));
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }

    private static int multARGBInts(int colorA, int colorB) {
        return ModelQuadUtil.mixARGBColors(colorA, colorB);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, float r, float g, float b, float[] brightnessTable, boolean colorize, int[] light, int overlay) {
        Matrix3fExtended matNormal = Matrix3fExtended.get(matrices.normal());
        Matrix4fExtended matPosition = Matrix4fExtended.get(matrices.pose());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            // The packed transformed normal vector
            var normal = matNormal.transformNormal(quad.getLightFace());

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = matPosition.transformVecX(x, y, z);
                float yt = matPosition.transformVecY(x, y, z);
                float zt = matPosition.transformVecZ(x, y, z);

                float fR;
                float fG;
                float fB;

                float brightness = brightnessTable[i];

                if (colorize) {
                    int color = quad.getColor(i);

                    float oR = ColorU8.byteToNormalizedFloat(ColorABGR.unpackRed(color));
                    float oG = ColorU8.byteToNormalizedFloat(ColorABGR.unpackGreen(color));
                    float oB = ColorU8.byteToNormalizedFloat(ColorABGR.unpackBlue(color));

                    fR = oR * brightness * r;
                    fG = oG * brightness * g;
                    fB = oB * brightness * b;
                } else {
                    fR = brightness * r;
                    fG = brightness * g;
                    fB = brightness * b;
                }

                int color = ColorABGR.pack(fR, fG, fB, 1.0F);

                ModelVertex.write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), overlay, ModelQuadUtil.mergeBakedLight(quad.getLight(i), light[i]), mergeNormalAndMult(quad.getForgeNormal(i), normal, matNormal));
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }
}
