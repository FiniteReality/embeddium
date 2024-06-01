package org.embeddedt.embeddium.api.math;

import com.mojang.math.Matrix4f;
import com.mojang.math.Quaternion;
import org.jetbrains.annotations.Contract;

public interface Matrix4fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);

    /**
     * Applies the specified translation to this matrix in-place.
     *
     * @param x The x-component of the translation
     * @param y The y-component of the translation
     * @param z The z-component of the translation
     */
    void translate(float x, float y, float z);

    /**
     * Applies this matrix transformation to the given input vector, returning the x-component. Avoids the lack of
     * struct types in Java and allows for allocation-free return.
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The x-component of the transformed input vector
     */
    float transformVecX(float x, float y, float z);

    /**
     * Applies this matrix transformation to the given input vector, returning the y-component. Avoids the lack of
     * struct types in Java and allows for allocation-free return.
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The y-component of the transformed input vector
     */
    float transformVecY(float x, float y, float z);

    /**
     * Applies this matrix transformation to the given input vector, returning the z-component. Avoids the lack of
     * struct types in Java and allows for allocation-free return.
     * @param x The x-component of the vector
     * @param y The y-component of the vector
     * @param z The z-component of the vector
     * @return The z-component of the transformed input vector
     */
    float transformVecZ(float x, float y, float z);

    float getA00();

    float getA10();

    float getA20();

    float getA30();

    float getA01();

    float getA11();

    float getA21();

    float getA31();

    float getA02();

    float getA12();

    float getA22();

    float getA32();

    float getA03();

    float getA13();

    float getA23();

    float getA33();

    @Contract(pure = true)
    static Matrix4fExtended get(Matrix4f matrix) {
        return (Matrix4fExtended)(Object)matrix;
    }
}
