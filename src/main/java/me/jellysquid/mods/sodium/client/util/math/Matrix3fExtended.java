package me.jellysquid.mods.sodium.client.util.math;

import com.mojang.math.Quaternion;
import com.mojang.math.Vector3f;
import net.minecraft.core.Direction;

public interface Matrix3fExtended {
    /**
     * Applies the specified rotation to this matrix in-place.
     *
     * @param quaternion The quaternion to rotate this matrix by
     */
    void rotate(Quaternion quaternion);

    int computeNormal(Direction dir);

    float transformVecX(float x, float y, float z);

    float transformVecY(float x, float y, float z);

    float transformVecZ(float x, float y, float z);

    default float transformVecX(Vector3f dir) {
        return this.transformVecX(dir.x(), dir.y(), dir.z());
    }

    default float transformVecY(Vector3f dir) {
        return this.transformVecY(dir.x(), dir.y(), dir.z());
    }

    default float transformVecZ(Vector3f dir) {
        return this.transformVecZ(dir.x(), dir.y(), dir.z());
    }
}
