package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.world.WorldSlice;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.client.model.data.ModelData;
import org.joml.Vector3f;
import org.joml.Vector3fc;

public class BlockRenderContext {
    private final WorldSlice world;

    private final BlockPos.Mutable pos = new BlockPos.Mutable();

    private final Vector3f origin = new Vector3f();

    private BlockState state;
    private BakedModel model;

    private long seed;

    private ModelData modelData;
    private RenderLayer renderLayer;


    public BlockRenderContext(WorldSlice world) {
        this.world = world;
    }

    public void update(BlockPos pos, BlockPos origin, BlockState state, BakedModel model, long seed, ModelData modelData, RenderLayer renderLayer) {
        this.pos.set(pos);
        this.origin.set(origin.getX(), origin.getY(), origin.getZ());

        this.state = state;
        this.model = model;

        this.seed = seed;

        this.modelData = modelData;
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
     * @return The additional data for model instance
     */
    public ModelData modelData() {
        return this.modelData;
    }

    /**
     * @return The render layer for model rendering
     */
    public RenderLayer renderLayer() {
        return this.renderLayer;
    }
}
