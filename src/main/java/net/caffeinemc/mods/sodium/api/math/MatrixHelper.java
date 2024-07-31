package net.caffeinemc.mods.sodium.api.math;

import com.mojang.blaze3d.vertex.PoseStack;
import net.caffeinemc.mods.sodium.api.util.NormI8;
import net.minecraft.core.Direction;
import org.joml.Math;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Implements optimized utilities for transforming vectors with a given matrix.
 *
 * Note: Brackets must be used carefully in the transform functions to ensure that floating-point errors are
 * the same as those produced by JOML, otherwise Z-fighting will occur.
 */
public class MatrixHelper {
    /**
     * @param mat The transformation matrix to apply to the normal
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed normal vector (in packed format)
     */
    public static int transformNormal(Matrix3f mat, float x, float y, float z) {
        // The transformed normal vector
        float nxt = transformNormalX(mat, x, y, z);
        float nyt = transformNormalY(mat, x, y, z);
        float nzt = transformNormalZ(mat, x, y, z);

        return NormI8.pack(nxt, nyt, nzt);
    }

    /**
     * @param mat The transformation matrix to apply to the normal
     * @param norm The normal vector to transform (in packed format)
     * @return The transformed normal vector (in packed format)
     */
    public static int transformNormal(Matrix3f mat, int norm) {
        // The unpacked normal vector
        float x = NormI8.unpackX(norm);
        float y = NormI8.unpackY(norm);
        float z = NormI8.unpackZ(norm);

        return transformNormal(mat, x, y, z);
    }

    /**
     * @param mat The transformation matrix to apply to the normal vector
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed X-coordinate for the normal vector
     */
    public static float transformNormalX(Matrix3f mat, float x, float y, float z) {
        return (mat.m00() * x) + ((mat.m10() * y) + (mat.m20() * z));
    }

    /**
     * @param mat The transformation matrix to apply to the normal vector
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed Y-coordinate for the normal vector
     */
    public static float transformNormalY(Matrix3f mat, float x, float y, float z) {
        return (mat.m01() * x) + ((mat.m11() * y) + (mat.m21() * z));
    }

    /**
     * @param mat The transformation matrix to apply to the normal vector
     * @param x The X-coordinate of the normal vector
     * @param y The Y-coordinate of the normal vector
     * @param z The Z-coordinate of the normal vector
     * @return The transformed Z-coordinate for the normal vector
     */
    public static float transformNormalZ(Matrix3f mat, float x, float y, float z) {
        return (mat.m02() * x) + ((mat.m12() * y) + (mat.m22() * z));
    }

    /**
     * @param mat The transformation matrix to apply to the vertex position
     * @param x The X-coordinate of the vertex position
     * @param y The Y-coordinate of the vertex position
     * @param z The Z-coordinate of the vertex position
     * @return The transformed X-coordinate for the vertex position
     */
    public static float transformPositionX(Matrix4f mat, float x, float y, float z) {
        return (mat.m00() * x) + ((mat.m10() * y) + ((mat.m20() * z) + mat.m30()));
    }

    /**
     * @param mat The transformation matrix to apply to the vertex position
     * @param x The X-coordinate of the vertex position
     * @param y The Y-coordinate of the vertex position
     * @param z The Z-coordinate of the vertex position
     * @return The transformed Y-coordinate for the vertex position
     */
    public static float transformPositionY(Matrix4f mat, float x, float y, float z) {
        return (mat.m01() * x) + ((mat.m11() * y) + ((mat.m21() * z) + mat.m31()));
    }

    /**
     * @param mat The transformation matrix to apply to the vertex position
     * @param x The X-coordinate of the vertex position
     * @param y The Y-coordinate of the vertex position
     * @param z The Z-coordinate of the vertex position
     * @return The transformed Z-coordinate for the vertex position
     */
    public static float transformPositionZ(Matrix4f mat, float x, float y, float z) {
        return (mat.m02() * x) + ((mat.m12() * y) + ((mat.m22() * z) + mat.m32()));
    }

    /**
     * Returns the transformed normal vector for a given unit vector (direction). This is significantly faster
     * than transforming the vector directly (i.e. with {@link Matrix3f#transform(Vector3f)}), as it can simply
     * extract the values from the provided matrix (rather than transforming the vertices.)
     *
     * @param matrix The transformation matrix
     * @param direction The unit vector (direction) to use
     * @return A transformed normal in packed format
     */
    public static int transformNormal(Matrix3f matrix, Direction direction) {
        return transformNormal(matrix, true, direction);
    }

    /**
     * Returns the transformed normal vector for a given unit vector (direction). This is significantly faster
     * than transforming the vector directly (i.e. with {@link Matrix3f#transform(Vector3f)}), as it can simply
     * extract the values from the provided matrix (rather than transforming the vertices.)
     *
     * @param matrix The transformation matrix
     * @param trustNormals Whether the calculated vector should be assumed to be normalized
     * @param direction The unit vector (direction) to use
     * @return A transformed normal in packed format
     */
    public static int transformNormal(Matrix3f matrix, boolean trustNormals, Direction direction) {
        float x, y, z;
        if (direction == Direction.DOWN) {
            x = -matrix.m10;
            y = -matrix.m11;
            z = -matrix.m12;
        } else if (direction == Direction.UP) {
            x = matrix.m10;
            y = matrix.m11;
            z = matrix.m12;
        } else if (direction == Direction.NORTH) {
            x = -matrix.m20;
            y = -matrix.m21;
            z = -matrix.m22;
        } else if (direction == Direction.SOUTH) {
            x = matrix.m20;
            y = matrix.m21;
            z = matrix.m22;
        } else if (direction == Direction.WEST) {
            x = -matrix.m00;
            y = -matrix.m01;
            z = -matrix.m02;
        } else if (direction == Direction.EAST) {
            x = matrix.m00;
            y = matrix.m01;
            z = matrix.m02;
        } else {
            throw new IllegalArgumentException("An incorrect direction enum was provided..");
        }

        if (!trustNormals) {
            float scalar = Math.invsqrt(Math.fma(x, x, Math.fma(y, y, z * z)));

            x *= scalar;
            y *= scalar;
            z *= scalar;
        }

        return NormI8.pack(x, y, z);
    }
}
