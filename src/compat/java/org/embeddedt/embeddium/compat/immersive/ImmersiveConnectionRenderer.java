package org.embeddedt.embeddium.compat.immersive;

import blusunrize.immersiveengineering.api.utils.ResettableLazy;
import blusunrize.immersiveengineering.api.wires.Connection;
import blusunrize.immersiveengineering.api.wires.ConnectionPoint;
import blusunrize.immersiveengineering.api.wires.GlobalWireNetwork;
import blusunrize.immersiveengineering.api.wires.WireCollisionData;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadWinding;
import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildBuffers;
import me.jellysquid.mods.sodium.client.render.chunk.compile.buffers.ChunkModelBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.DefaultMaterials;
import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.builder.ChunkMeshBufferBuilder;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import net.caffeinemc.mods.sodium.api.util.ColorARGB;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.SectionPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.Mth;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.phys.Vec3;
import org.embeddedt.embeddium.api.ChunkMeshEvent;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * This class, in combination with ImmersiveEmptyChunkChecker, implements support
 * for IE wire rendering in Embeddium, on versions that predate the standard chunk
 * meshing event in NeoForge. This code originates from https://github.com/malte0811/rubie,
 * made by one of the IE authors, and was distributed in Rubidium with permission
 * under LGPL3.
 */
public class ImmersiveConnectionRenderer implements ResourceManagerReloadListener {
    private static final LoadingCache<SegmentsKey, List<RenderedSegment>> SEGMENT_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(120, TimeUnit.SECONDS)
            .build(CacheLoader.from(ImmersiveConnectionRenderer::renderSegmentForCache));
    private static final ResettableLazy<TextureAtlasSprite> WIRE_TEXTURE = new ResettableLazy<>(
            () -> Minecraft.getInstance().getModelManager()
                    .getAtlas(TextureAtlas.LOCATION_BLOCKS)
                    .getSprite(rl("block/wire"))
    );

    public static ResourceLocation rl(String path)
	{
		return new ResourceLocation("immersiveengineering", path);
	}

    static void meshAppendEvent(ChunkMeshEvent event) {
        if(ImmersiveEmptyChunkChecker.hasWires(event.getSectionOrigin())) {
            event.addMeshAppender(ctx -> renderConnectionsInSection(ctx.sodiumBuildBuffers(), ctx.blockRenderView(), ctx.sectionOrigin()));
        }
    }

    @Override
    public void onResourceManagerReload(@Nonnull ResourceManager pResourceManager) {
        WIRE_TEXTURE.reset();
        resetCache();
    }

    public static void resetCache() {
        SEGMENT_CACHE.invalidateAll();
    }

    private static final Material MATERIAL = DefaultMaterials.SOLID;

    public static void renderConnectionsInSection(
            ChunkBuildBuffers buffers, BlockAndTintGetter region, SectionPos section
    ) {
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(Minecraft.getInstance().level);
        List<WireCollisionData.ConnectionSegments> connectionParts = globalNet.getCollisionData().getWiresIn(section);
        if (connectionParts == null || connectionParts.isEmpty()) {
            return;
        }
        RenderType renderType = RenderType.solid();
        ChunkModelBuilder builder = buffers.get(DefaultMaterials.forRenderLayer(renderType));
        int originX = section.minBlockX();
        int originY = section.minBlockY();
        int originZ = section.minBlockZ();
        for (WireCollisionData.ConnectionSegments connection : connectionParts) {
            ConnectionPoint connectionOrigin = connection.connection().getEndA();
            renderSegments(
                    builder, connection, region,
                    connectionOrigin.getX() - originX,
                    connectionOrigin.getY() - originY,
                    connectionOrigin.getZ() - originZ
            );
        }
    }

    public static void renderSegments(
            ChunkModelBuilder out, WireCollisionData.ConnectionSegments toRender,
            BlockAndTintGetter level, int offX, int offY, int offZ
    ) {
        Connection connection = toRender.connection();
        int colorRGB = connection.type.getColour(connection);
        int colorBGR = ColorARGB.toABGR(colorRGB, 0xFF);
        double radius = connection.type.getRenderDiameter() / 2;
        List<RenderedSegment> segments = SEGMENT_CACHE.getUnchecked(new SegmentsKey(
                radius, colorBGR, connection.getCatenaryData(),
                toRender.firstPointToRender(), toRender.lastPointToRender()
        ));
        int lastLight = 0;
        for (int startPoint = 0; startPoint < segments.size(); ++startPoint) {
            var renderedSegment = segments.get(startPoint);
            if (startPoint == 0) {
                lastLight = getLight(connection, renderedSegment.offsetStart, level);
            }
            int nextLight = getLight(connection, renderedSegment.offsetEnd, level);
            renderedSegment.render(lastLight, nextLight, offX, offY, offZ, out);
            lastLight = nextLight;
        }
    }

    private static List<RenderedSegment> renderSegmentForCache(SegmentsKey key) {
        List<RenderedSegment> segments = new ArrayList<>(key.endIndex() - key.beginIndex());
        for (int i = key.beginIndex(); i < key.endIndex(); ++i) {
            segments.add(renderSegmentForCache(key, i));
        }
        return segments;
    }

    private static RenderedSegment renderSegmentForCache(SegmentsKey key, int startIndex) {
        Connection.CatenaryData catenaryData = key.catenaryShape();
        Vec3 start = key.catenaryShape().getRenderPoint(startIndex);
        Vec3 end = key.catenaryShape().getRenderPoint(startIndex + 1);
        Vec3 horNormal;
        if (key.catenaryShape().isVertical()) {
            horNormal = new Vec3(1, 0, 0);
        } else {
            horNormal = new Vec3(-catenaryData.delta().z, 0, catenaryData.delta().x).normalize();
        }
        Vec3 verticalNormal = start.subtract(end).cross(horNormal).normalize();
        Vec3 horRadius = horNormal.scale(key.radius());
        Vec3 verticalRadius = verticalNormal.scale(-key.radius());
        return new RenderedSegment(
                renderQuad(start, end, horRadius, key.color()),
                renderQuad(start, end, verticalRadius, key.color()),
                flooredVec3(start.x, start.y, start.z),
                flooredVec3(end.x, end.y, end.z)
        );
    }

    private static Vec3i flooredVec3(double x, double y, double z) {
        return new Vec3i(Mth.floor(x), Mth.floor(y), Mth.floor(z));
    }

    private static int getLight(Connection connection, Vec3i point, BlockAndTintGetter level) {
        return LevelRenderer.getLightColor(level, connection.getEndA().position().offset(point));
    }

    private static Quad renderQuad(Vec3 start, Vec3 end, Vec3 radius, int color) {
    	TextureAtlasSprite texture = WIRE_TEXTURE.get();
        return new Quad(
                vertex(start.add(radius), texture.getU0(), texture.getV0(), color, true),
                vertex(end.add(radius), texture.getU1(), texture.getV0(), color, false),
                vertex(end.subtract(radius), texture.getU1(), texture.getV1(), color, false),
                vertex(start.subtract(radius), texture.getU0(), texture.getV1(), color, true)
        );
    }

    private static Vertex vertex(Vec3 point, double u, double v, int color, boolean lightForStart) {
        return new Vertex(
                (float) point.x, (float) point.y, (float) point.z, (float) u, (float) v, color, lightForStart
        );
    }

    private record SegmentsKey(
            double radius, int color, Connection.CatenaryData catenaryShape, int beginIndex, int endIndex
    ) {
    }

    private record Vertex(
            float posX, float posY, float posZ,
            float texU, float texV,
            int color,
            boolean lightForStart
    ) {
        static final ThreadLocal<ChunkVertexEncoder.Vertex[]> vertexHolder = ThreadLocal.withInitial(() -> new ChunkVertexEncoder.Vertex[1]);

        void write(
                ChunkMeshBufferBuilder vertexSink, int offX, int offY, int offZ, int lightStart, int lightEnd
        ) {
            ChunkVertexEncoder.Vertex sodiumVertex = new ChunkVertexEncoder.Vertex();
            sodiumVertex.x = offX + posX;
            sodiumVertex.y = offY + posY;
            sodiumVertex.z = offZ + posZ;
            sodiumVertex.color = color;
            sodiumVertex.u = texU;
            sodiumVertex.v = texV;
            sodiumVertex.light = lightForStart ? lightStart : lightEnd;
            ChunkVertexEncoder.Vertex[] array = vertexHolder.get();
            array[0] = sodiumVertex;
            vertexSink.push(array, MATERIAL);
        }
    }

    private record Quad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        void write(
                ChunkModelBuilder out, int offX, int offY, int offZ, int lightStart, int lightEnd
        ) {
            var vertexSink = out.getVertexBuffer(ModelQuadFacing.UNASSIGNED);
            int quadStart = vertexSink.count();
            // clockwise
            v0.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            v1.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            v2.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            v3.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            // counter-clockwise
            v0.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            v3.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            v2.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
            v1.write(vertexSink, offX, offY, offZ, lightStart, lightEnd);
        }
    }

    private record RenderedSegment(Quad quadA, Quad quadB, Vec3i offsetStart, Vec3i offsetEnd) {
        public void render(
                int lightStart, int lightEnd, int offX, int offY, int offZ, ChunkModelBuilder out
        ) {
            quadA.write(out, offX, offY, offZ, lightStart, lightEnd);
            quadB.write(out, offX, offY, offZ, lightStart, lightEnd);
        }
    }
}
