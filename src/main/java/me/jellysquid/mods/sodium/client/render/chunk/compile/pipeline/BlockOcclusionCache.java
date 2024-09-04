package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import it.unimi.dsi.fastutil.objects.Object2ByteLinkedOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The block occlusion cache is responsible for performing occlusion testing of neighboring block faces.
 */
public class BlockOcclusionCache {
    private static final byte UNCACHED_VALUE = (byte) 127;

    private final Object2ByteLinkedOpenHashMap<CachedOcclusionShapeTest> map;
    private final CachedOcclusionShapeTest cachedTest = new CachedOcclusionShapeTest();
    private final BlockPos.MutableBlockPos cpos = new BlockPos.MutableBlockPos();

    public BlockOcclusionCache() {
        this.map = new Object2ByteLinkedOpenHashMap<>(2048, 0.5F);
        this.map.defaultReturnValue(UNCACHED_VALUE);
    }

    /**
     * @param selfState The state of the block in the world
     * @param view The world view for this render context
     * @param pos The position of the block
     * @param facing The facing direction of the side to check
     * @return True if the block side facing {@param dir} is not occluded, otherwise false
     */
    public boolean shouldDrawSide(BlockState selfState, BlockGetter view, BlockPos pos, Direction facing) {
        BlockPos.MutableBlockPos adjPos = this.cpos;
        adjPos.set(pos.getX() + facing.getStepX(), pos.getY() + facing.getStepY(), pos.getZ() + facing.getStepZ());

        BlockState adjState = view.getBlockState(adjPos);

        if (selfState.skipRendering(adjState, facing) || (adjState.hidesNeighborFace(view, adjPos, selfState, facing.getOpposite()) && selfState.supportsExternalFaceHiding())) {
            // Explicitly asked to skip rendering this face
            return false;
        } else if (adjState.canOcclude()) {
            VoxelShape selfShape = selfState.getFaceOcclusionShape(view, pos, facing);
            VoxelShape adjShape = adjState.getFaceOcclusionShape(view, adjPos, facing.getOpposite());

            if (selfShape == Shapes.block() && adjShape == Shapes.block()) {
                // If both blocks use full-cube occlusion shapes, then the neighbor certainly occludes us, and we
                // shouldn't render this face
                return false;
            } else if (selfShape.isEmpty()) {
                // If our occlusion shape is empty, then we cannot be occluded by anything, and we should render
                // this face
                return true;
            }

            // Consult the occlusion cache & do the voxel shape calculations if necessary
            return this.calculate(selfShape, adjShape);
        } else {
            // The neighboring block never occludes, we need to render this face
            return true;
        }
    }

    private boolean calculate(VoxelShape selfShape, VoxelShape adjShape) {
        CachedOcclusionShapeTest cache = this.cachedTest;
        cache.a = selfShape;
        cache.b = adjShape;
        cache.updateHash();

        byte cached = this.map.getByte(cache);

        if (cached != UNCACHED_VALUE) {
            return cached == 1;
        }

        boolean ret = Shapes.joinIsNotEmpty(selfShape, adjShape, BooleanOp.ONLY_FIRST);

        this.map.put(cache.copy(), (byte) (ret ? 1 : 0));

        if (this.map.size() > 2048) {
            this.map.removeFirstByte();
        }

        return ret;
    }

    private static final class CachedOcclusionShapeTest {
        private VoxelShape a, b;
        private int hashCode;

        private CachedOcclusionShapeTest() {

        }

        private CachedOcclusionShapeTest(VoxelShape a, VoxelShape b, int hashCode) {
            this.a = a;
            this.b = b;
            this.hashCode = hashCode;
        }

        public void updateHash() {
            int result = System.identityHashCode(this.a);
            result = 31 * result + System.identityHashCode(this.b);

            this.hashCode = result;
        }

        public CachedOcclusionShapeTest copy() {
            return new CachedOcclusionShapeTest(this.a, this.b, this.hashCode);
        }

        @Override
        public boolean equals(Object o) {
            if (o instanceof CachedOcclusionShapeTest that) {
                return this.a == that.a &&
                        this.b == that.b;
            }

            return false;
        }

        @Override
        public int hashCode() {
            return this.hashCode;
        }
    }
}
