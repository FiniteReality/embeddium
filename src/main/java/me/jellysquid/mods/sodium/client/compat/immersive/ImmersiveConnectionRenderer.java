package me.jellysquid.mods.sodium.client.compat.immersive;

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
import me.jellysquid.mods.sodium.client.render.chunk.format.ModelVertexSink;
import me.jellysquid.mods.sodium.client.util.color.ColorARGB;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.texture.Sprite;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.SynchronousResourceReloader;
import net.minecraft.screen.PlayerScreenHandler;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.BlockRenderView;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class ImmersiveConnectionRenderer implements SynchronousResourceReloader {
    private static final LoadingCache<SegmentsKey, List<RenderedSegment>> SEGMENT_CACHE = CacheBuilder.newBuilder()
            .expireAfterAccess(120, TimeUnit.SECONDS)
            .build(CacheLoader.from(ImmersiveConnectionRenderer::renderSegmentForCache));
    private static final ResettableLazy<Sprite> WIRE_TEXTURE = new ResettableLazy<>(
            () -> MinecraftClient.getInstance().getBakedModelManager()
                    .getAtlas(PlayerScreenHandler.BLOCK_ATLAS_TEXTURE)
                    .getSprite(rl("block/wire"))
    );
    
    public static Identifier rl(String path)
	{
		return new Identifier("immersiveengineering", path);
	}

    @Override
    public void reload(@Nonnull ResourceManager pResourceManager) {
        WIRE_TEXTURE.reset();
        resetCache();
    }

    public static void resetCache() {
        SEGMENT_CACHE.invalidateAll();
    }

    public static void renderConnectionsInSection(
            ChunkBuildBuffers buffers, BlockRenderView region, ChunkSectionPos section
    ) {
        GlobalWireNetwork globalNet = GlobalWireNetwork.getNetwork(MinecraftClient.getInstance().world);
        List<WireCollisionData.ConnectionSegments> connectionParts = globalNet.getCollisionData().getWiresIn(section);
        if (connectionParts == null || connectionParts.isEmpty()) {
            return;
        }
        RenderLayer renderType = RenderLayer.getSolid();
        ChunkModelBuilder builder = buffers.get(renderType);
        int originX = section.getMinX();
        int originY = section.getMinY();
        int originZ = section.getMinZ();
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
            BlockRenderView level, int offX, int offY, int offZ
    ) {
        Connection connection = toRender.connection();
        int colorRGB = connection.type.getColour(connection);
        int colorBGR = ColorARGB.toABGR(colorRGB);
        double radius = connection.type.getRenderDiameter() / 2;
        var vertices = out.getVertexSink();
        List<RenderedSegment> segments = SEGMENT_CACHE.getUnchecked(new SegmentsKey(
                radius, colorBGR, connection.getCatenaryData(),
                toRender.firstPointToRender(), toRender.lastPointToRender()
        ));
        vertices.ensureCapacity(2 * 4 * segments.size());
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
        vertices.flush();
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
        Vec3d start = key.catenaryShape().getRenderPoint(startIndex);
        Vec3d end = key.catenaryShape().getRenderPoint(startIndex + 1);
        Vec3d horNormal;
        if (key.catenaryShape().isVertical()) {
            horNormal = new Vec3d(1, 0, 0);
        } else {
            horNormal = new Vec3d(-catenaryData.delta().z, 0, catenaryData.delta().x).normalize();
        }
        Vec3d verticalNormal = start.subtract(end).crossProduct(horNormal).normalize();
        Vec3d horRadius = horNormal.multiply(key.radius());
        Vec3d verticalRadius = verticalNormal.multiply(-key.radius());
        return new RenderedSegment(
                renderQuad(start, end, horRadius, key.color()),
                renderQuad(start, end, verticalRadius, key.color()),
                new Vec3i(start.x, start.y, start.z),
                new Vec3i(end.x, end.y, end.z)
        );
    }

    private static int getLight(Connection connection, Vec3i point, BlockRenderView level) {
        return WorldRenderer.getLightmapCoordinates(level, connection.getEndA().position().add(point));
    }

    private static Quad renderQuad(Vec3d start, Vec3d end, Vec3d radius, int color) {
    	Sprite texture = WIRE_TEXTURE.get();
        return new Quad(
                vertex(start.add(radius), texture.getMinU(), texture.getMinV(), color, true),
                vertex(end.add(radius), texture.getMaxU(), texture.getMinV(), color, false),
                vertex(end.subtract(radius), texture.getMaxU(), texture.getMaxV(), color, false),
                vertex(start.subtract(radius), texture.getMinU(), texture.getMaxV(), color, true)
        );
    }

    private static Vertex vertex(Vec3d point, double u, double v, int color, boolean lightForStart) {
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
        void write(
                ModelVertexSink vertexSink, int offX, int offY, int offZ, int lightStart, int lightEnd, int chunkId
        ) {
            vertexSink.writeVertex(
                    offX + posX, offY + posY, offZ + posZ,
                    color, texU, texV, lightForStart ? lightStart : lightEnd, chunkId
            );
        }
    }

    private record Quad(Vertex v0, Vertex v1, Vertex v2, Vertex v3) {
        void write(
                ChunkModelBuilder out, int offX, int offY, int offZ, int lightStart, int lightEnd
        ) {
            var vertexSink = out.getVertexSink();
            int quadStart = vertexSink.getVertexCount();
            v0.write(vertexSink, offX, offY, offZ, lightStart, lightEnd, out.getChunkId());
            v1.write(vertexSink, offX, offY, offZ, lightStart, lightEnd, out.getChunkId());
            v2.write(vertexSink, offX, offY, offZ, lightStart, lightEnd, out.getChunkId());
            v3.write(vertexSink, offX, offY, offZ, lightStart, lightEnd, out.getChunkId());
            var indexBuffer = out.getIndexBufferBuilder(ModelQuadFacing.UNASSIGNED);
            indexBuffer.add(quadStart, ModelQuadWinding.CLOCKWISE);
            indexBuffer.add(quadStart, ModelQuadWinding.COUNTERCLOCKWISE);
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