package org.embeddedt.embeddium.impl.render.immediate.model;

import com.mojang.blaze3d.vertex.PoseStack;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.util.ModelQuadUtil;
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.ColorU8;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.vertex.format.common.ModelVertex;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;

public class BakedModelEncoder {
    /**
     * Use the packed normal and transform it if set, otherwise use the precalculated and multiplied normal.
     */
    private static int mergeNormalAndMult(int packed, int calc, Matrix3f matNormal) {
        if((packed & 0xFFFFFF) == 0)
            return calc;
        return MatrixHelper.transformNormal(matNormal, packed);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, int color, int light, int overlay, boolean colorize) {
        Matrix3f matNormal = matrices.normal();
        Matrix4f matPosition = matrices.pose();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            // The packed transformed normal vector
            var normal = MatrixHelper.transformNormal(matNormal, matrices.trustedNormals, quad.getLightFace());

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

                ModelVertex.write(ptr, xt, yt, zt, colorize ? multARGBInts(quad.getColor(i), color) : color, quad.getTexU(i), quad.getTexV(i), overlay, ModelQuadUtil.mergeBakedLight(quad.getLight(i), quad.getVanillaLightEmission(), light), mergeNormalAndMult(quad.getForgeNormal(i), normal, matNormal));
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }

    private static int multARGBInts(int colorA, int colorB) {
        return ModelQuadUtil.mixARGBColors(colorA, colorB);
    }

    public static void writeQuadVertices(VertexBufferWriter writer, PoseStack.Pose matrices, ModelQuadView quad, float r, float g, float b, float a, float[] brightnessTable, boolean colorize, int[] light, int overlay) {
        Matrix3f matNormal = matrices.normal();
        Matrix4f matPosition = matrices.pose();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ModelVertex.STRIDE);
            long ptr = buffer;

            // The packed transformed normal vector
            var normal = MatrixHelper.transformNormal(matNormal, matrices.trustedNormals, quad.getLightFace());

            for (int i = 0; i < 4; i++) {
                // The position vector
                float x = quad.getX(i);
                float y = quad.getY(i);
                float z = quad.getZ(i);

                // The transformed position vector
                float xt = MatrixHelper.transformPositionX(matPosition, x, y, z);
                float yt = MatrixHelper.transformPositionY(matPosition, x, y, z);
                float zt = MatrixHelper.transformPositionZ(matPosition, x, y, z);

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

                int color = ColorABGR.pack(fR, fG, fB, a);

                ModelVertex.write(ptr, xt, yt, zt, color, quad.getTexU(i), quad.getTexV(i), overlay, ModelQuadUtil.mergeBakedLight(quad.getLight(i), quad.getVanillaLightEmission(), light[i]), mergeNormalAndMult(quad.getForgeNormal(i), normal, matNormal));
                ptr += ModelVertex.STRIDE;
            }

            writer.push(stack, buffer, 4, ModelVertex.FORMAT);
        }
    }
}
