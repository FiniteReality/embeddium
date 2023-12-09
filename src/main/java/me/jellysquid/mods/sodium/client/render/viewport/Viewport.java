package me.jellysquid.mods.sodium.client.render.viewport;

import me.jellysquid.mods.sodium.client.render.viewport.frustum.Frustum;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraftforge.common.extensions.IForgeBlockEntity;
import org.joml.Vector3d;

public final class Viewport {
    private final Frustum frustum;
    private final CameraTransform transform;

    private final ChunkSectionPos chunkCoords;
    private final BlockPos blockCoords;

    public Viewport(Frustum frustum, Vector3d position) {
        this.frustum = frustum;
        this.transform = new CameraTransform(position.x, position.y, position.z);

        this.chunkCoords = ChunkSectionPos.from(
                ChunkSectionPos.getSectionCoord(position.x),
                ChunkSectionPos.getSectionCoord(position.y),
                ChunkSectionPos.getSectionCoord(position.z)
        );

        this.blockCoords = BlockPos.ofFloored(position.x, position.y, position.z);
    }

    public boolean isBoxVisible(Box box) {
        if (box.equals(IForgeBlockEntity.INFINITE_EXTENT_AABB)) {
            return true;
        }

        return this.frustum.testAab(
                (float)(box.minX - this.transform.intX) - this.transform.fracX,
                (float)(box.minY - this.transform.intY) - this.transform.fracY,
                (float)(box.minZ - this.transform.intZ) - this.transform.fracZ,
                (float)(box.maxX - this.transform.intX) - this.transform.fracX,
                (float)(box.maxY - this.transform.intY) - this.transform.fracY,
                (float)(box.maxZ - this.transform.intZ) - this.transform.fracZ
        );
    }

    public boolean isBoxVisible(int intX, int intY, int intZ, float radius) {
        float floatX = (intX - this.transform.intX) - this.transform.fracX;
        float floatY = (intY - this.transform.intY) - this.transform.fracY;
        float floatZ = (intZ - this.transform.intZ) - this.transform.fracZ;

        return this.frustum.testAab(
                floatX - radius,
                floatY - radius,
                floatZ - radius,

                floatX + radius,
                floatY + radius,
                floatZ + radius
        );
    }

    public CameraTransform getTransform() {
        return this.transform;
    }

    public ChunkSectionPos getChunkCoord() {
        return this.chunkCoords;
    }

    public BlockPos getBlockCoord() {
        return this.blockCoords;
    }
}
