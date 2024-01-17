package me.jellysquid.mods.sodium.client.render.pipeline;

import org.apache.commons.lang3.mutable.MutableFloat;
import org.apache.commons.lang3.mutable.MutableInt;
import org.jetbrains.annotations.Nullable;

import me.jellysquid.mods.sodium.client.model.light.LightMode;
import me.jellysquid.mods.sodium.client.model.light.LightPipeline;
import me.jellysquid.mods.sodium.client.model.light.LightPipelineProvider;
import me.jellysquid.mods.sodium.client.model.light.data.QuadLightData;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuad;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadView;
import me.jellysquid.mods.sodium.client.model.quad.ModelQuadViewMutable;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorBlender;
import me.jellysquid.mods.sodium.client.model.quad.blender.ColorSampler;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFlags;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.Norm3b;
import me.jellysquid.mods.sodium.client.util.color.ColorABGR;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.BlockPos.MutableBlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.SupportType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.embeddedt.embeddium.render.fluid.EmbeddiumFluidSpriteCache;

public class FluidRenderer {
    // TODO: allow this to be changed by vertex format
    // TODO: move fluid rendering to a separate render pass and control glPolygonOffset and glDepthFunc to fix this properly
    private static final float EPSILON = 0.001f;

    private final BlockPos.MutableBlockPos scratchPos = new BlockPos.MutableBlockPos();
    private final MutableFloat scratchHeight = new MutableFloat(0);
    private final MutableInt scratchSamples = new MutableInt();

    private final ModelQuadViewMutable quad = new ModelQuad();

    private final LightPipelineProvider lighters;
    private final ColorBlender colorBlender;

    // Cached wrapper type that adapts FluidRenderHandler to support QuadColorProvider<FluidState>
    private final ForgeFluidColorizerAdapter forgeColorProviderAdapter = new ForgeFluidColorizerAdapter();

    private final QuadLightData quadLightData = new QuadLightData();
    private final int[] quadColors = new int[4];

    private final EmbeddiumFluidSpriteCache fluidSpriteCache = new EmbeddiumFluidSpriteCache();

    public FluidRenderer(LightPipelineProvider lighters, ColorBlender colorBlender) {
        int normal = Norm3b.pack(0.0f, 1.0f, 0.0f);

        for (int i = 0; i < 4; i++) {
            this.quad.setNormal(i, normal);
        }

        this.lighters = lighters;
        this.colorBlender = colorBlender;
    }

    private boolean isFluidOccluded(BlockAndTintGetter world, int x, int y, int z, Direction dir, Fluid fluid) {
        BlockPos pos = this.scratchPos.set(x, y, z);
        BlockState blockState = world.getBlockState(pos);
        BlockPos adjPos = this.scratchPos.set(x + dir.getStepX(), y + dir.getStepY(), z + dir.getStepZ());

        if (blockState.canOcclude()) {
            return world.getFluidState(adjPos).getType().isSame(fluid) || blockState.isFaceSturdy(world,pos,dir, SupportType.FULL);
            // fluidlogged or next to water, occlude sides that are solid or the same liquid
            }
        return world.getFluidState(adjPos).getType().isSame(fluid);
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

    public boolean render(BlockAndTintGetter world, FluidState fluidState, BlockPos pos, BlockPos offset, ChunkModelBuilder buffers) {
        int posX = pos.getX();
        int posY = pos.getY();
        int posZ = pos.getZ();

        Fluid fluid = fluidState.getType();

        boolean sfUp = this.isFluidOccluded(world, posX, posY, posZ, Direction.UP, fluid);
        boolean sfDown = this.isFluidOccluded(world, posX, posY, posZ, Direction.DOWN, fluid) ||
                !this.isSideExposed(world, posX, posY, posZ, Direction.DOWN, 0.8888889F);
        boolean sfNorth = this.isFluidOccluded(world, posX, posY, posZ, Direction.NORTH, fluid);
        boolean sfSouth = this.isFluidOccluded(world, posX, posY, posZ, Direction.SOUTH, fluid);
        boolean sfWest = this.isFluidOccluded(world, posX, posY, posZ, Direction.WEST, fluid);
        boolean sfEast = this.isFluidOccluded(world, posX, posY, posZ, Direction.EAST, fluid);

        if (sfUp && sfDown && sfEast && sfWest && sfNorth && sfSouth) {
            return false;
        }

        boolean isWater = fluidState.is(FluidTags.WATER);

        ColorSampler<FluidState> colorizer = this.createColorProviderAdapter(world, pos, fluidState);

        TextureAtlasSprite[] sprites = fluidSpriteCache.getSprites(world, pos, fluidState);

        boolean rendered = false;

        float fluidHeight = this.fluidHeight(world, fluid, pos);
        float h1, h2, h3, h4;
        if (fluidHeight >= 1.0f) {
            h1 = 1.0f;
            h2 = 1.0f;
            h3 = 1.0f;
            h4 = 1.0f;
        } else {
            var scratchPos = new BlockPos.MutableBlockPos();
            float north1 = this.fluidHeight(world, fluid, scratchPos.setWithOffset(pos, Direction.NORTH));
            float south1 = this.fluidHeight(world, fluid, scratchPos.setWithOffset(pos, Direction.SOUTH));
            float east1 = this.fluidHeight(world, fluid, scratchPos.setWithOffset(pos, Direction.EAST));
            float west1 = this.fluidHeight(world, fluid, scratchPos.setWithOffset(pos, Direction.WEST));
            h1 = this.fluidCornerHeight(world, fluid, fluidHeight, north1, west1, scratchPos.set(pos).move(Direction.NORTH).move(Direction.WEST));
            h2 = this.fluidCornerHeight(world, fluid, fluidHeight, south1, west1, scratchPos.set(pos).move(Direction.SOUTH).move(Direction.WEST));
            h3 = this.fluidCornerHeight(world, fluid, fluidHeight, south1, east1, scratchPos.set(pos).move(Direction.SOUTH).move(Direction.EAST));
            h4 = this.fluidCornerHeight(world, fluid, fluidHeight, north1, east1, scratchPos.set(pos).move(Direction.NORTH).move(Direction.EAST));
        }

        float yOffset = sfDown ? 0.0F : EPSILON;

        final ModelQuadViewMutable quad = this.quad;

        LightMode lightMode = isWater && Minecraft.useAmbientOcclusion() ? LightMode.SMOOTH : LightMode.FLAT;
        LightPipeline lighter = this.lighters.getLighter(lightMode);

        quad.setFlags(0);

        if (!sfUp && this.isSideExposed(world, posX, posY, posZ, Direction.UP, Math.min(Math.min(h1, h2), Math.min(h3, h4)))) {
            h1 -= EPSILON;
            h2 -= EPSILON;
            h3 -= EPSILON;
            h4 -= EPSILON;

            Vec3 velocity = fluidState.getFlow(world, pos);

            TextureAtlasSprite sprite;
            ModelQuadFacing facing;
            float u1, u2, u3, u4;
            float v1, v2, v3, v4;

            if (velocity.x == 0.0D && velocity.z == 0.0D) {
                sprite = sprites[0];
                facing = ModelQuadFacing.UP;
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

            this.setVertex(quad, 0, 0.0f, h1, 0.0f, u1, v1);
            this.setVertex(quad, 1, 0.0f, h2, 1.0F, u2, v2);
            this.setVertex(quad, 2, 1.0F, h3, 1.0F, u3, v3);
            this.setVertex(quad, 3, 1.0F, h4, 0.0f, u4, v4);

            this.calculateQuadColors(quad, world, pos, lighter, Direction.UP, 1.0F, colorizer, fluidState);

            int vertexStart = this.writeVertices(buffers, offset, quad);

            buffers.getIndexBufferBuilder(facing)
                    .add(vertexStart, ModelQuadWinding.CLOCKWISE);

            if (fluidState.shouldRenderBackwardUpFace(world, this.scratchPos.set(posX, posY + 1, posZ))) {
                buffers.getIndexBufferBuilder(ModelQuadFacing.DOWN)
                        .add(vertexStart, ModelQuadWinding.COUNTERCLOCKWISE);
            }

            rendered = true;
        }

        if (!sfDown) {
            TextureAtlasSprite sprite = sprites[0];

            float minU = sprite.getU0();
            float maxU = sprite.getU1();
            float minV = sprite.getV0();
            float maxV = sprite.getV1();
            quad.setSprite(sprite);

            this.setVertex(quad, 0, 0.0f, yOffset, 1.0F, minU, maxV);
            this.setVertex(quad, 1, 0.0f, yOffset, 0.0f, minU, minV);
            this.setVertex(quad, 2, 1.0F, yOffset, 0.0f, maxU, minV);
            this.setVertex(quad, 3, 1.0F, yOffset, 1.0F, maxU, maxV);

            this.calculateQuadColors(quad, world, pos, lighter, Direction.DOWN, 1.0F, colorizer, fluidState);

            int vertexStart = this.writeVertices(buffers, offset, quad);

            buffers.getIndexBufferBuilder(ModelQuadFacing.DOWN)
                    .add(vertexStart, ModelQuadWinding.CLOCKWISE);

            rendered = true;
        }

        quad.setFlags(ModelQuadFlags.IS_ALIGNED);

        for (Direction dir : DirectionUtil.HORIZONTAL_DIRECTIONS) {
            float c1;
            float c2;
            float x1;
            float z1;
            float x2;
            float z2;

            switch (dir) {
                case NORTH:
                    if (sfNorth) {
                        continue;
                    }

                    c1 = h1;
                    c2 = h4;
                    x1 = 0.0f;
                    x2 = 1.0F;
                    z1 = EPSILON;
                    z2 = z1;
                    break;
                case SOUTH:
                    if (sfSouth) {
                        continue;
                    }

                    c1 = h3;
                    c2 = h2;
                    x1 = 1.0F;
                    x2 = 0.0f;
                    z1 = 1.0f - EPSILON;
                    z2 = z1;
                    break;
                case WEST:
                    if (sfWest) {
                        continue;
                    }

                    c1 = h2;
                    c2 = h1;
                    x1 = EPSILON;
                    x2 = x1;
                    z1 = 1.0F;
                    z2 = 0.0f;
                    break;
                case EAST:
                    if (sfEast) {
                        continue;
                    }

                    c1 = h4;
                    c2 = h3;
                    x1 = 1.0f - EPSILON;
                    x2 = x1;
                    z1 = 0.0f;
                    z2 = 1.0F;
                    break;
                default:
                    continue;
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

                    if (adjBlock.shouldDisplayFluidOverlay(world, adjPos, fluidState)) {
                        sprite = sprites[2];
                        if(sprite != null)
                        	isOverlay = true;
                        else
                        	sprite = sprites[1];
                    }
                }

                float u1 = sprite.getU(0.0D);
                float u2 = sprite.getU(8.0D);
                float v1 = sprite.getV((1.0F - c1) * 16.0F * 0.5F);
                float v2 = sprite.getV((1.0F - c2) * 16.0F * 0.5F);
                float v3 = sprite.getV(8.0D);

                quad.setSprite(sprite);

                this.setVertex(quad, 0, x2, c2, z2, u2, v2);
                this.setVertex(quad, 1, x2, yOffset, z2, u2, v3);
                this.setVertex(quad, 2, x1, yOffset, z1, u1, v3);
                this.setVertex(quad, 3, x1, c1, z1, u1, v1);

                float br = dir.getAxis() == Direction.Axis.Z ? 0.8F : 0.6F;

                ModelQuadFacing facing = ModelQuadFacing.fromDirection(dir);

                this.calculateQuadColors(quad, world, pos, lighter, dir, br, colorizer, fluidState);

                int vertexStart = this.writeVertices(buffers, offset, quad);

                buffers.getIndexBufferBuilder(facing)
                        .add(vertexStart, ModelQuadWinding.CLOCKWISE);

                if (!isOverlay) {
                    buffers.getIndexBufferBuilder(facing.getOpposite())
                            .add(vertexStart, ModelQuadWinding.COUNTERCLOCKWISE);
                }

                rendered = true;
            }
        }

        return rendered;
    }

    private ColorSampler<FluidState> createColorProviderAdapter(BlockAndTintGetter view, BlockPos pos, FluidState state) {
        ForgeFluidColorizerAdapter adapter = this.forgeColorProviderAdapter;
        adapter.setHandler(view, pos, state);

        return adapter;
    }

    private void calculateQuadColors(ModelQuadView quad, BlockAndTintGetter world, BlockPos pos, LightPipeline lighter, Direction dir, float brightness,
                                     ColorSampler<FluidState> colorSampler, FluidState fluidState) {
        QuadLightData light = this.quadLightData;
        lighter.calculate(quad, pos, light, null, dir, false);

        int[] biomeColors = this.colorBlender.getColors(world, pos, quad, colorSampler, fluidState);

        for (int i = 0; i < 4; i++) {
            this.quadColors[i] = ColorABGR.mul(biomeColors != null ? biomeColors[i] : 0xFFFFFFFF, light.br[i] * brightness);
        }
    }

    private int writeVertices(ChunkModelBuilder builder, BlockPos offset, ModelQuadView quad) {
        ModelVertexSink vertices = builder.getVertexSink();
        vertices.ensureCapacity(4);

        int vertexStart = vertices.getVertexCount();

        for (int i = 0; i < 4; i++) {
            float x = quad.getX(i);
            float y = quad.getY(i);
            float z = quad.getZ(i);

            int color = this.quadColors[i];

            float u = quad.getTexU(i);
            float v = quad.getTexV(i);

            int light = this.quadLightData.lm[i];

            vertices.writeVertex(offset, x, y, z, color, u, v, light, builder.getChunkId());
        }

        vertices.flush();

        TextureAtlasSprite sprite = quad.getSprite();

        if (sprite != null) {
            builder.addSprite(sprite);
        }

        return vertexStart;
    }

    private void setVertex(ModelQuadViewMutable quad, int i, float x, float y, float z, float u, float v) {
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
            float height = this.fluidHeight(world, fluid, blockPos);

            if (height >= 1.0f) {
                return 1.0f;
            }

            modifyHeight(scratchHeight, scratchSamples, height);
        }

        modifyHeight(scratchHeight, scratchSamples, fluidHeight);
        modifyHeight(scratchHeight, scratchSamples, fluidHeightY);
        modifyHeight(scratchHeight, scratchSamples, fluidHeightX);

        float result = scratchHeight.floatValue() / scratchSamples.intValue();
        scratchHeight.setValue(0);
        scratchSamples.setValue(0);

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

    private float fluidHeight(BlockAndTintGetter world, Fluid fluid, BlockPos blockPos) {
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

    private static class ForgeFluidColorizerAdapter implements ColorSampler<FluidState> {
        private BlockAndTintGetter world;
        private BlockPos pos;
        private FluidState state;

        public void setHandler(BlockAndTintGetter world, BlockPos pos, FluidState state) {
            this.world = world;
            this.pos = pos;
            this.state = state;
        }
        
        @Override
        public int getColor(FluidState state, @Nullable BlockAndTintGetter world, @Nullable BlockPos pos, int tintIndex) {
            if (this.world == null || this.state == null) {
                return -1;
            }

            return state.getType().getAttributes().getColor(world, pos);
        }
    }
}
