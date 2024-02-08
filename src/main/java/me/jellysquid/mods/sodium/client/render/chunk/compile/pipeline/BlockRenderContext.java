package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.world.WorldSliceLocal;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BlockRenderContext {
    private final WorldSlice world;
    private final WorldSliceLocal localSlice;

    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    private final Vector3f origin = new Vector3f();

    private BlockState state;
    private BakedModel model;

    private long seed;

    private RenderType renderLayer;


    public BlockRenderContext(WorldSlice world) {
        this.world = world;
        this.localSlice = new WorldSliceLocal(world);
    }

    public void update(BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed, RenderType renderLayer) {
        this.pos.set(pos);
        this.origin.set(origin.getX(), origin.getY(), origin.getZ());

        this.state = state;
        this.model = model;

        this.seed = seed;

        this.renderLayer = renderLayer;
    }

    /**
     * @return The position (in world space) of the block being rendered
     */
    public BlockPos pos() {
        return this.pos;
    }

    /**
     * @return The world which the block is being rendered from
     */
    public WorldSlice world() {
        return this.world;
    }

    /**
     * @return The world which the block is being rendered from. Guaranteed to be a new object for each subchunk.
     */
    public WorldSliceLocal localSlice() {
        return this.localSlice;
    }

    /**
     * @return The state of the block being rendered
     */
    public BlockState state() {
        return this.state;
    }

    /**
     * @return The model used for this block
     */
    public BakedModel model() {
        return this.model;
    }

    /**
     * @return The origin of the block within the model
     */
    public Vector3fc origin() {
        return this.origin;
    }

    /**
     * @return The PRNG seed for rendering this block
     */
    public long seed() {
        return this.seed;
    }

    /**
     * @return The render layer for model rendering
     */
    public RenderType renderLayer() {
        return this.renderLayer;
    }
}
