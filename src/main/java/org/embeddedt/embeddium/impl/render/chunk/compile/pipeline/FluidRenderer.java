package org.embeddedt.embeddium.impl.render.chunk.compile.pipeline;

import org.embeddedt.embeddium.api.util.ColorMixer;
import org.embeddedt.embeddium.impl.model.light.LightMode;
import org.embeddedt.embeddium.impl.model.light.LightPipeline;
import org.embeddedt.embeddium.impl.model.light.LightPipelineProvider;
import org.embeddedt.embeddium.impl.model.light.data.QuadLightData;
import org.embeddedt.embeddium.impl.model.quad.ModelQuad;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadView;
import org.embeddedt.embeddium.impl.model.quad.ModelQuadViewMutable;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.embeddedt.embeddium.impl.model.color.ColorProviderRegistry;
import org.embeddedt.embeddium.impl.model.color.ColorProvider;
import org.embeddedt.embeddium.impl.model.color.DefaultColorProviders;
import org.embeddedt.embeddium.impl.render.ShaderModBridge;
import org.embeddedt.embeddium.impl.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;
import org.embeddedt.embeddium.impl.render.chunk.compile.buffers.ChunkModelBuilder;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.DefaultMaterials;
import org.embeddedt.embeddium.impl.render.chunk.terrain.material.Material;
import org.embeddedt.embeddium.impl.render.chunk.vertex.format.ChunkVertexEncoder;
import org.embeddedt.embeddium.impl.world.WorldSlice;
import org.embeddedt.embeddium.impl.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.embeddedt.embeddium.impl.render.chunk.compile.GlobalChunkBuildContext;
import org.embeddedt.embeddium.impl.render.fluid.EmbeddiumFluidSpriteCache;

import java.util.Objects;

/**
 * The Embeddium equivalent to vanilla's ModelBlockRenderer. It is the complement of {@link BlockRenderer} for
 * emitting fluid geometry.
 * <p>
 * This class does not need to be thread-safe, as a separate instance is allocated per meshing thread.
 */
public class FluidRenderer {
    // TODO: allow this to be changed by vertex format
    // TODO: move fluid rendering to a separate render pass and control glPolygonOffset and glDepthFunc to fix this properly
    private static final float EPSILON = 0.001f;
    private static final float ALIGNED_EQUALS_EPSILON = 0.011f;

    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private final MutableFloat scratchHeight = new MutableFloat(0);
    private final MutableInt scratchSamples = new MutableInt();

    private final ModelQuadViewMutable quad = new ModelQuad();

    private final LightPipelineProvider lighters;

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];

    private final ChunkVertexEncoder.Vertex[] vertices = ChunkVertexEncoder.Vertex.uninitializedQuad();
    private final ColorProviderRegistry colorProviderRegistry;

    private final EmbeddiumFluidSpriteCache fluidSpriteCache = new EmbeddiumFluidSpriteCache();

    private final ChunkColorWriter colorEncoder = ChunkColorWriter.get();

    public FluidRenderer(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters) {
        this.quad.setLightFace(Direction.UP);

        this.lighters = lighters;
        this.colorProviderRegistry = colorProviderRegistry;
    }

    /**
     * {@return true if a fluid's face is occluded by surrounding block/fluid geometry and thus does not need to be rendered}
     * @param world the block getter that can be used to obtain more context about surrounding blocks
     * @param x the X coordinate of the current fluid
     * @param y the Y coordinate of the current fluid
     * @param z the Z coordinate of the current fluid
     * @param dir the face to check for occlusion on
     * @param fluid the type of the current fluid
     */
    private boolean isFluidOccluded(BlockAndTintGetter world, int x, int y, int z, Direction dir, Fluid fluid) {
        // Check if the fluid adjacent to us in the given direction is the same
        if (world.getFluidState(this.scratchPos.set(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ())).getType().isSame(fluid)) {
            return true;
        }

        // Stricter than vanilla: check whether the containing block can occlude, has a sturdy face on the given side,
        // and has a solid occlusion shape. If so, assume the fluid inside is not visible on that side.
        // This avoids rendering the top face of water inside an upper waterlogged slab, for instance.
        BlockPos pos = this.scratchPos.set(x, y, z);
        BlockState blockState = world.getBlockState(pos);

        if (!blockState.canOcclude() || !blockState.isFaceSturdy(world, pos, dir, SupportType.FULL)) {
            // The blockstate we're inside doesn't occlude or isn't sturdy on this side, so it cannot possibly
            // be hiding the fluid
            return false;
        }

        VoxelShape sideShape = blockState.getFaceOcclusionShape(world, pos, dir);
        if (sideShape == Shapes.block()) {
            // The face fills the 1x1 area, so the fluid is occluded
            return true;
        } else if (sideShape == Shapes.empty()) {
            // The face does not exist, so the fluid is not occluded
            return false;
        } else {
            // Check if the face fills the 1x1 area
            return Block.isShapeFullBlock(sideShape);
        }
    }

    private boolean isSideExposed(BlockAndTintGetter world, int x, int y, int z, Direction dir, float height) {
        BlockPos pos = this.scratchPos.set(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());
        BlockState blockState = world.getBlockState(pos);

        if (blockState.canOcclude()) {
            VoxelShape shape = blockState.getOcclusionShape(world, pos);

            // Hoist these checks to avoid allocating the shape below
            if (shape == Shapes.block()) {
                // The top face always be inset, so if the shape above is a full cube it can't possibly occlude
                return dir == Direction.UP;
            } else if (shape.isEmpty()) {
                return true;
            }

            VoxelShape threshold = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, height, 1.0D);

            return !Shapes.blockOccudes(threshold, shape, dir);
        }

        return true;
    }

    private static boolean isAlignedEquals(float a, float b) {
        return Math.abs(a - b) <= ALIGNED_EQUALS_EPSILON;
    }

    public void render(WorldSlice world, FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forFluidState(fluidState);
        var meshBuilder = buffers.get(material);

        // Embeddium: Apply Forge's hook for fluid rendering
        var context = Objects.requireNonNull(GlobalChunkBuildContext.get());
        context.setCaptureAdditionalSprites(true);

        boolean skipDefaultRendering;
        try(var consumer = meshBuilder.asVertexConsumer(material)) {
            skipDefaultRendering = IClientFluidTypeExtensions.of(fluidState).renderFluid(fluidState, world, blockPos, consumer, world.getBlockState(blockPos));
        }

        for(TextureAtlasSprite sprite : context.getAdditionalCapturedSprites()) {
            if (sprite != null) {
                meshBuilder.addSprite(sprite);
            }
        }

        context.setCaptureAdditionalSprites(false);

        if(skipDefaultRendering) {
            return;
        }

        int posX = blockPos.getX();
        int posY = blockPos.getY();
        int posZ = blockPos.getZ();

        Fluid fluid = fluidState.getType();

        // Each variable represents whether fluid rendering should be skipped on this side
        boolean sfUp = this.isFluidOccluded(world, posX, posY, posZ, Direction.UP, fluid);
        boolean sfDown = this.isFluidOccluded(world, posX, posY, posZ, Direction.DOWN, fluid) ||
                !this.isSideExposed(world, posX, posY, posZ, Direction.DOWN, 0.8888889F);
        boolean sfNorth = this.isFluidOccluded(world, posX, posY, posZ, Direction.NORTH, fluid);
        boolean sfSouth = this.isFluidOccluded(world, posX, posY, posZ, Direction.SOUTH, fluid);
        boolean sfWest = this.isFluidOccluded(world, posX, posY, posZ, Direction.WEST, fluid);
        boolean sfEast = this.isFluidOccluded(world, posX, posY, posZ, Direction.EAST, fluid);

        if (sfUp && sfDown && sfEast && sfWest && sfNorth && sfSouth) {
            return;
        }

        // LVT name kept for 1.20.1 in case a mixin captures it, the meaning of this variable is now "does the fluid
        // support AO"
        boolean isWater = fluid.getFluidType().getLightLevel(fluidState, world, blockPos) == 0;

        final ColorProvider<FluidState> colorProvider = this.getColorProvider(fluid);

        TextureAtlasSprite[] sprites = fluidSpriteCache.getSprites(world, blockPos, fluidState);

        float fluidHeight = this.fluidHeight(world, fluid, blockPos, Direction.UP);
        float northWestHeight, southWestHeight, southEastHeight, northEastHeight;
        if (fluidHeight >= 1.0f) {
            northWestHeight = 1.0f;
            southWestHeight = 1.0f;
            southEastHeight = 1.0f;
            northEastHeight = 1.0f;
        } else {
            var scratchPos = new BlockPos.MutableBlockPos();
            float heightNorth = this.fluidHeight(world, fluid, scratchPos.setWithOffset(blockPos, Direction.NORTH), Direction.NORTH);
            float heightSouth = this.fluidHeight(world, fluid, scratchPos.setWithOffset(blockPos, Direction.SOUTH), Direction.SOUTH);
            float heightEast = this.fluidHeight(world, fluid, scratchPos.setWithOffset(blockPos, Direction.EAST), Direction.EAST);
            float heightWest = this.fluidHeight(world, fluid, scratchPos.setWithOffset(blockPos, Direction.WEST), Direction.WEST);
            northWestHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightNorth, heightWest, scratchPos.set(blockPos)
                    .move(Direction.NORTH)
                    .move(Direction.WEST));
            southWestHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightSouth, heightWest, scratchPos.set(blockPos)
                    .move(Direction.SOUTH)
                    .move(Direction.WEST));
            southEastHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightSouth, heightEast, scratchPos.set(blockPos)
                    .move(Direction.SOUTH)
                    .move(Direction.EAST));
            northEastHeight = this.fluidCornerHeight(world, fluid, fluidHeight, heightNorth, heightEast, scratchPos.set(blockPos)
                    .move(Direction.NORTH)
                    .move(Direction.EAST));
        }
        float yOffset = sfDown ? 0.0F : EPSILON;

        final ModelQuadViewMutable quad = this.quad;

        LightMode lightMode = isWater && Minecraft.useAmbientOcclusion() ? LightMode.SMOOTH : LightMode.FLAT;
        LightPipeline lighter = this.lighters.getLighter(lightMode);

        quad.setFlags(ModelQuadFlags.IS_VANILLA_SHADED);

        if (!sfUp && this.isSideExposed(world, posX, posY, posZ, Direction.UP, Math.min(Math.min(northWestHeight, southWestHeight), Math.min(southEastHeight, northEastHeight)))) {
            northWestHeight -= EPSILON;
            southWestHeight -= EPSILON;
            southEastHeight -= EPSILON;
            northEastHeight -= EPSILON;

            Vec3 velocity = fluidState.getFlow(world, blockPos);

            TextureAtlasSprite sprite;
            ModelQuadFacing facing;
            float u1, u2, u3, u4;
            float v1, v2, v3, v4;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                sprite = sprites[0];
                facing = ModelQuadFacing.POS_Y;
                u1 = sprite.getU(0.0f);
                v1 = sprite.getV(0.0f);
                u2 = u1;
                v2 = sprite.getV(1.0f);
                u3 = sprite.getU(1.0f);
                v3 = v2;
                u4 = u3;
                v4 = v1;
            } else {
                sprite = sprites[1];
                facing = ModelQuadFacing.UNASSIGNED;
                float dir = (float) Mth.atan2(velocity.z, velocity.x) - (1.5707964f);
                float sin = Mth.sin(dir) * 0.25F;
                float cos = Mth.cos(dir) * 0.25F;
                u1 = sprite.getU(0.5F + (-cos - sin));
                v1 = sprite.getV(0.5F + -cos + sin);
                u2 = sprite.getU(0.5F + -cos + sin);
                v2 = sprite.getV(0.5F + cos + sin);
                u3 = sprite.getU(0.5F + cos + sin);
                v3 = sprite.getV(0.5F + (cos - sin));
                u4 = sprite.getU(0.5F + (cos - sin));
                v4 = sprite.getV(0.5F + (-cos - sin));
            }

            float uAvg = (u1 + u2 + u3 + u4) / 4.0F;
            float vAvg = (v1 + v2 + v3 + v4) / 4.0F;
            float s3 = sprites[0].uvShrinkRatio();

            u1 = Mth.lerp(s3, u1, uAvg);
            u2 = Mth.lerp(s3, u2, uAvg);
            u3 = Mth.lerp(s3, u3, uAvg);
            u4 = Mth.lerp(s3, u4, uAvg);
            v1 = Mth.lerp(s3, v1, vAvg);
            v2 = Mth.lerp(s3, v2, vAvg);
            v3 = Mth.lerp(s3, v3, vAvg);
            v4 = Mth.lerp(s3, v4, vAvg);

            quad.setSprite(sprite);

            // top surface alignedness is calculated with a more relaxed epsilon
            boolean aligned = isAlignedEquals(northEastHeight, northWestHeight)
                    && isAlignedEquals(northWestHeight, southEastHeight)
                    && isAlignedEquals(southEastHeight, southWestHeight)
                    && isAlignedEquals(southWestHeight, northEastHeight);

            boolean creaseNorthEastSouthWest = aligned
                    || northEastHeight > northWestHeight && northEastHeight > southEastHeight
                    || northEastHeight < northWestHeight && northEastHeight < southEastHeight
                    || southWestHeight > northWestHeight && southWestHeight > southEastHeight
                    || southWestHeight < northWestHeight && southWestHeight < southEastHeight;

            if (creaseNorthEastSouthWest) {
                setVertex(quad, 1, 0.0f, northWestHeight, 0.0f, u1, v1);
                setVertex(quad, 2, 0.0f, southWestHeight, 1.0F, u2, v2);
                setVertex(quad, 3, 1.0F, southEastHeight, 1.0F, u3, v3);
                setVertex(quad, 0, 1.0F, northEastHeight, 0.0f, u4, v4);
            } else {
                setVertex(quad, 0, 0.0f, northWestHeight, 0.0f, u1, v1);
                setVertex(quad, 1, 0.0f, southWestHeight, 1.0F, u2, v2);
                setVertex(quad, 2, 1.0F, southEastHeight, 1.0F, u3, v3);
                setVertex(quad, 3, 1.0F, northEastHeight, 0.0f, u4, v4);
            }

            this.updateQuad(quad, world, blockPos, lighter, Direction.UP, 1.0F, colorProvider, fluidState);
            this.writeQuad(meshBuilder, material, offset, quad, facing, false);

            if (fluidState.shouldRenderBackwardUpFace(world, this.scratchPos.set(posX, posY + 1, posZ))) {
                this.writeQuad(meshBuilder, material, offset, quad,
                        ModelQuadFacing.NEG_Y, true);

            }

        }

        if (!sfDown) {
            TextureAtlasSprite sprite = sprites[0];

            float minU = sprite.getU0();
            float maxU = sprite.getU1();
            float minV = sprite.getV0();
            float maxV = sprite.getV1();
            quad.setSprite(sprite);

            setVertex(quad, 0, 0.0f, yOffset, 1.0F, minU, maxV);
            setVertex(quad, 1, 0.0f, yOffset, 0.0f, minU, minV);
            setVertex(quad, 2, 1.0F, yOffset, 0.0f, maxU, minV);
            setVertex(quad, 3, 1.0F, yOffset, 1.0F, maxU, maxV);

            this.updateQuad(quad, world, blockPos, lighter, Direction.DOWN, 1.0F, colorProvider, fluidState);
            this.writeQuad(meshBuilder, material, offset, quad, ModelQuadFacing.NEG_Y, false);

        }

        quad.setFlags(ModelQuadFlags.IS_VANILLA_SHADED | ModelQuadFlags.IS_PARALLEL | ModelQuadFlags.IS_ALIGNED);

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            float c1;
            float c2;
            float x1;
            float z1;
            float x2;
            float z2;

            switch (dir) {
                case NORTH -> {
                    if (sfNorth) {
                        continue;
                    }
                    c1 = northWestHeight;
                    c2 = northEastHeight;
                    x1 = 0.0f;
                    x2 = 1.0F;
                    z1 = EPSILON;
                    z2 = z1;
                }
                case SOUTH -> {
                    if (sfSouth) {
                        continue;
                    }
                    c1 = southEastHeight;
                    c2 = southWestHeight;
                    x1 = 1.0F;
                    x2 = 0.0f;
                    z1 = 1.0f - EPSILON;
                    z2 = z1;
                }
                case WEST -> {
                    if (sfWest) {
                        continue;
                    }
                    c1 = southWestHeight;
                    c2 = northWestHeight;
                    x1 = EPSILON;
                    x2 = x1;
                    z1 = 1.0F;
                    z2 = 0.0f;
                }
                case EAST -> {
                    if (sfEast) {
                        continue;
                    }
                    c1 = northEastHeight;
                    c2 = southEastHeight;
                    x1 = 1.0f - EPSILON;
                    x2 = x1;
                    z1 = 0.0f;
                    z2 = 1.0F;
                }
                default -> {
                    continue;
                }
            }

            if (this.isSideExposed(world, posX, posY, posZ, dir, Math.max(c1, c2))) {
                int adjX = posX + dir.getStepX();
                int adjY = posY + dir.getStepY();
                int adjZ = posZ + dir.getStepZ();

                TextureAtlasSprite sprite = sprites[1];

                boolean isOverlay = false;

                if (sprites.length > 2) {
                    BlockPos adjPos = this.scratchPos.set(adjX, adjY, adjZ);
                    BlockState adjBlock = world.getBlockState(adjPos);

                    if (sprites[2] != null && adjBlock.shouldDisplayFluidOverlay(world, adjPos, fluidState)) {
                        sprite = sprites[2];
                        isOverlay = true;
                    }
                }

                float u1 = sprite.getU(0.0F);
                float u2 = sprite.getU(0.5F);
                float v1 = sprite.getV((1.0F - c1) * 0.5F);
                float v2 = sprite.getV((1.0F - c2) * 0.5F);
                float v3 = sprite.getV(0.5F);

                quad.setSprite(sprite);

                setVertex(quad, 0, x2, c2, z2, u2, v2);
                setVertex(quad, 1, x2, yOffset, z2, u2, v3);
                setVertex(quad, 2, x1, yOffset, z1, u1, v3);
                setVertex(quad, 3, x1, c1, z1, u1, v1);

                float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

                ModelQuadFacing facing = ModelQuadFacing.fromDirection(dir);

                this.updateQuad(quad, world, blockPos, lighter, dir, br, colorProvider, fluidState);
                this.writeQuad(meshBuilder, material, offset, quad, facing, false);

                if (!isOverlay) {
                    this.writeQuad(meshBuilder, material, offset, quad, facing.getOpposite(), true);
                }

            }
        }
    }

    private ColorProvider<FluidState> getColorProvider(Fluid fluid) {
        var override = this.colorProviderRegistry.getColorProvider(fluid);

        if (override != null) {
            return override;
        }
        
        return DefaultColorProviders.getFluidProvider();
    }

    private void updateQuad(ModelQuadView quad, WorldSlice world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness,
                            ColorProvider<FluidState> colorProvider, FluidState fluidState) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos, light, null, dir, false);

        colorProvider.getColors(world, pos, fluidState, quad, this.quadColors);

        // multiply the per-vertex color against the combined brightness
        // the combined brightness is the per-vertex brightness multiplied by the block's brightness
        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = colorEncoder.writeColor(this.quadColors[i], light.br[i] * brightness);
        }
    }

    private void writeQuad(ChunkModelBuilder builder, Material material, BlockPos offset, ModelQuadView quad,
                           ModelQuadFacing facing, boolean flip) {
        var vertices = this.vertices;

        for (int i = 0; i < 4; i++) {
            var out = vertices[flip ? (3 - i + 1) & 0b11 : i];
            out.x = offset.getX() + quad.getX(i);
            out.y = offset.getY() + quad.getY(i);
            out.z = offset.getZ() + quad.getZ(i);

            out.color = this.quadColors[i];
            out.u = quad.getTexU(i);
            out.v = quad.getTexV(i);
            out.light = this.quadLightData.lm[i];
        }

        TextureAtlasSprite sprite = quad.getSprite();

        if (sprite != null) {
            builder.addSprite(sprite);
        }

        var vertexBuffer = builder.getVertexBuffer(facing);
        vertexBuffer.push(vertices, material);
    }

    private static void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
        quad.setX(i, x);
        quad.setY(i, y);
        quad.setZ(i, z);
        quad.setTexU(i, u);
        quad.setTexV(i, v);
    }

    private float fluidCornerHeight(BlockAndTintGetter world, Fluid fluid, float fluidHeight, float fluidHeightX, float fluidHeightY, BlockPos blockPos) {
        if (fluidHeightY >= 1.0f || fluidHeightX >= 1.0f) {
            return 1.0f;
        }

        if (fluidHeightY > 0.0f || fluidHeightX > 0.0f) {
            float height = this.fluidHeight(world, fluid, blockPos, Direction.UP);

            if (height >= 1.0f) {
                return 1.0f;
            }

            this.modifyHeight(this.scratchHeight, this.scratchSamples, height);
        }

        this.modifyHeight(this.scratchHeight, this.scratchSamples, fluidHeight);
        this.modifyHeight(this.scratchHeight, this.scratchSamples, fluidHeightY);
        this.modifyHeight(this.scratchHeight, this.scratchSamples, fluidHeightX);

        float result = this.scratchHeight.floatValue() / this.scratchSamples.intValue();
        this.scratchHeight.setValue(0);
        this.scratchSamples.setValue(0);

        return result;
    }

    private void modifyHeight(MutableFloat totalHeight, MutableInt samples, float target) {
        if (target >= 0.8f) {
            totalHeight.add(target * 10.0f);
            samples.add(10);
        } else if (target >= 0.0f) {
            totalHeight.add(target);
            samples.increment();
        }
    }

    private float fluidHeight(BlockAndTintGetter world, Fluid fluid, BlockPos blockPos, Direction direction) {
        BlockState blockState = world.getBlockState(blockPos);
        FluidState fluidState = blockState.getFluidState();

        if (fluid.isSame(fluidState.getType())) {
            FluidState fluidStateUp = world.getFluidState(blockPos.above());

            if (fluid.isSame(fluidStateUp.getType())) {
                return 1.0f;
            } else {
                return fluidState.getOwnHeight();
            }
        }
        if (!blockState.isSolid()) {
            return 0.0f;
        }
        return -1.0f;
    }
}
