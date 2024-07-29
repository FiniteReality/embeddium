package me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline;

import me.jellysquid.mods.sodium.client.compat.ccl.SinkingVertexBuilder;
import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.model.color.ColorProviderRegistry;
import me.jellysquid.mods.sodium.client.model.color.ColorProvider;
import me.jellysquid.mods.sodium.client.model.color.DefaultColorProviders;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import me.jellysquid.mods.sodium.client.world.WorldSlice;
import me.jellysquid.mods.sodium.client.util.DirectionUtil;
import net.caffeinemc.mods.sodium.api.util.ColorMixer;
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
import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.embeddedt.embeddium.impl.render.chunk.compile.GlobalChunkBuildContext;
import org.embeddedt.embeddium.render.chunk.ChunkColorWriter;
import org.embeddedt.embeddium.render.fluid.EmbeddiumFluidSpriteCache;
import org.embeddedt.embeddium.tags.EmbeddiumTags;

import java.util.Objects;

public class FluidRenderer {
    // TODO: allow this to be changed by vertex format
    // TODO: move fluid rendering to a separate render pass and control glPolygonOffset and glDepthFunc to fix this properly
    private static final float EPSILON = 0.001f;

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

    private final SinkingVertexBuilder fluidVertexBuilder = new SinkingVertexBuilder();

    private final ChunkColorWriter colorEncoder = ChunkColorWriter.get();
;
    public FluidRenderer(ColorProviderRegistry colorProviderRegistry, LightPipelineProvider lighters) {
        this.quad.setLightFace(Direction.UP);

        this.lighters = lighters;
        this.colorProviderRegistry = colorProviderRegistry;
    }

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

    private void renderVanilla(WorldSlice world, FluidState fluidState, BlockPos blockPos, ChunkModelBuilder buffers, Material material) {
        // Call vanilla fluid renderer and capture the results
        var context = Objects.requireNonNull(GlobalChunkBuildContext.get());
        context.setCaptureAdditionalSprites(true);
        fluidVertexBuilder.reset();
        Minecraft.getInstance().getBlockRenderer().renderLiquid(blockPos, world, fluidVertexBuilder, fluidState);
        fluidVertexBuilder.flush(buffers, material, 0, 0, 0);

        var sprites = context.getAdditionalCapturedSprites();

        for(TextureAtlasSprite sprite : sprites) {
            if (sprite != null) {
                buffers.addSprite(sprite);
            }
        }

        context.setCaptureAdditionalSprites(false);
    }

    public void render(WorldSlice world, FluidState fluidState, BlockPos blockPos, BlockPos offset, ChunkBuildBuffers buffers) {
        var material = DefaultMaterials.forFluidState(fluidState);
        var meshBuilder = buffers.get(material);

        // Embeddium: Delegate to vanilla liquid renderer if fluid has this tag.
        if(false) { // fluidState.getType().is(EmbeddiumTags.RENDERS_WITH_VANILLA)) {
            renderVanilla(world, fluidState, blockPos, meshBuilder, material);
            return;
        }

        int posX = blockPos.getX();
        int posY = blockPos.getY();
        int posZ = blockPos.getZ();

        Fluid fluid = fluidState.getType();

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

        boolean isWater = fluidState.is(FluidTags.WATER);

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

        quad.setFlags(0);

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
                u1 = sprite.getU(0.0D);
                v1 = sprite.getV(0.0D);
                u2 = u1;
                v2 = sprite.getV(16.0D);
                u3 = sprite.getU(16.0D);
                v3 = v2;
                u4 = u3;
                v4 = v1;
            } else {
                sprite = sprites[1];
                facing = ModelQuadFacing.UNASSIGNED;
                float dir = (float) Mth.atan2(velocity.z, velocity.x) - (1.5707964f);
                float sin = Mth.sin(dir) * 0.25F;
                float cos = Mth.cos(dir) * 0.25F;
                u1 = sprite.getU(8.0F + (-cos - sin) * 16.0F);
                v1 = sprite.getV(8.0F + (-cos + sin) * 16.0F);
                u2 = sprite.getU(8.0F + (-cos + sin) * 16.0F);
                v2 = sprite.getV(8.0F + (cos + sin) * 16.0F);
                u3 = sprite.getU(8.0F + (cos + sin) * 16.0F);
                v3 = sprite.getV(8.0F + (cos - sin) * 16.0F);
                u4 = sprite.getU(8.0F + (cos - sin) * 16.0F);
                v4 = sprite.getV(8.0F + (-cos - sin) * 16.0F);
            }

            float uAvg = (u1 + u2 + u3 + u4) / 4.0F;
            float vAvg = (v1 + v2 + v3 + v4) / 4.0F;
            float s1 = (float) sprites[0].getWidth() / (sprites[0].getU1() - sprites[0].getU0());
            float s2 = (float) sprites[0].getHeight() / (sprites[0].getV1() - sprites[0].getV0());
            float s3 = 4.0F / Math.max(s2, s1);

            u1 = Mth.lerp(s3, u1, uAvg);
            u2 = Mth.lerp(s3, u2, uAvg);
            u3 = Mth.lerp(s3, u3, uAvg);
            u4 = Mth.lerp(s3, u4, uAvg);
            v1 = Mth.lerp(s3, v1, vAvg);
            v2 = Mth.lerp(s3, v2, vAvg);
            v3 = Mth.lerp(s3, v3, vAvg);
            v4 = Mth.lerp(s3, v4, vAvg);

            quad.setSprite(sprite);

            setVertex(quad, 0, 0.0f, northWestHeight, 0.0f, u1, v1);
            setVertex(quad, 1, 0.0f, southWestHeight, 1.0F, u2, v2);
            setVertex(quad, 2, 1.0F, southEastHeight, 1.0F, u3, v3);
            setVertex(quad, 3, 1.0F, northEastHeight, 0.0f, u4, v4);

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

        quad.setFlags(ModelQuadFlags.IS_PARALLEL | ModelQuadFlags.IS_ALIGNED);

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

                float u1 = sprite.getU(0.0D);
                float u2 = sprite.getU(8.0D);
                float v1 = sprite.getV((1.0F - c1) * 16.0F * 0.5F);
                float v2 = sprite.getV((1.0F - c2) * 16.0F * 0.5F);
                float v3 = sprite.getV(8.0D);

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
            var out = vertices[flip ? 3 - i : i];
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
        if (!blockState.getMaterial().isSolid()) {
            return 0.0f;
        }
        return -1.0f;
    }
}
