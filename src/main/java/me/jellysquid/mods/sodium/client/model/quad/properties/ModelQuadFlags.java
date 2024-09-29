package me.jellysquid.mods.sodium.client.model.quad.properties;

import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;

public class ModelQuadFlags {
    /**
     * Indicates that the quad does not fully cover the given face for the model.
     */
    public static final int IS_PARTIAL = 0b001;

    /**
     * Indicates that the quad is parallel to its light face.
     */
    public static final int IS_PARALLEL = 0b010;

    /**
     * Indicates that the quad is aligned to the block grid.
     * This flag is only set if {@link #IS_PARALLEL} is set.
     */
    public static final int IS_ALIGNED = 0b100;

    /**
     * Indicates that the quad should be shaded using vanilla's getShade logic and the light face, rather than
     * the normals of each vertex.
     */
    public static final int IS_VANILLA_SHADED = 0b1000;
    /**
     * Indicates that the particle sprite on this quad can be trusted to be the only sprite it shows.
     */
    public static final int IS_TRUSTED_SPRITE = (1 << 4);
    /**
     * Indicates that the flags are populated for the quad.
     */
    public static final int IS_POPULATED = (1 << 31);

    /**
     * @return True if the bit-flag of {@link ModelQuadFlags} contains the given flag
     */
    public static boolean contains(int flags, int mask) {
        return (flags & mask) != 0;
    }

    /**
     * Calculates the properties of the given quad. This data is used later by the light pipeline in order to make
     * certain optimizations.
     */
    public static int getQuadFlags(ModelQuadView quad, Direction face) {
        float minX = 32.0F;
        float minY = 32.0F;
        float minZ = 32.0F;

        float maxX = -32.0F;
        float maxY = -32.0F;
        float maxZ = -32.0F;

        int numVertices = 4;
        if (quad instanceof BakedQuad bakedQuad) {
            numVertices = Math.min(numVertices, bakedQuad.getVertices().length / 8);
        }

        float lX = Float.NaN, lY = Float.NaN, lZ = Float.NaN;
        boolean degenerate = false;

        for (int i = 0; i < numVertices; ++i) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            minX = Math.min(minX, x);
            minY = Math.min(minY, y);
            minZ = Math.min(minZ, z);
            maxX = Math.max(maxX, x);
            maxY = Math.max(maxY, y);
            maxZ = Math.max(maxZ, z);

            if(x == lX && y == lY && z == lZ) {
                degenerate = true;
            } else {
                lX = x;
                lY = y;
                lZ = z;
            }
        }

        boolean partial = degenerate || (switch (face.getAxis()) {
            case X -> minY >= 0.0001f || minZ >= 0.0001f || maxY <= 0.9999F || maxZ <= 0.9999F;
            case Y -> minX >= 0.0001f || minZ >= 0.0001f || maxX <= 0.9999F || maxZ <= 0.9999F;
            case Z -> minX >= 0.0001f || minY >= 0.0001f || maxX <= 0.9999F || maxY <= 0.9999F;
        });

        boolean parallel = switch(face.getAxis()) {
            case X -> minX == maxX;
            case Y -> minY == maxY;
            case Z -> minZ == maxZ;
        };

        boolean aligned = parallel && switch (face) {
            case DOWN -> minY < 0.0001f;
            case UP -> maxY > 0.9999F;
            case NORTH -> minZ < 0.0001f;
            case SOUTH -> maxZ > 0.9999F;
            case WEST -> minX < 0.0001f;
            case EAST -> maxX > 0.9999F;
        };

        int flags = 0;

        if (partial) {
            flags |= IS_PARTIAL;
        }

        if (parallel) {
            flags |= IS_PARALLEL;
        }

        if (aligned) {
            flags |= IS_ALIGNED;
        }

        flags |= IS_POPULATED;

        return flags;
    }
}