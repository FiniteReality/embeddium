package me.jellysquid.mods.sodium.client.render.chunk.vertex.builder;

import it.unimi.dsi.fastutil.floats.FloatArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexEncoder;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3f;

public class TranslucentQuadAnalyzer {
    // X/Y/Z for each quad center
    private final FloatArrayList quadCenters = new FloatArrayList();
    private final Vector3f[] vertexPositions = new Vector3f[4];
    private int currentVertex;

    public TranslucentQuadAnalyzer() {
        for(int i = 0; i < 4; i++) {
            vertexPositions[i] = new Vector3f();
        }
    }

    public record SortState(float[] centers) {}

    @Nullable
    public SortState getSortState() {
        var centerArray = quadCenters.toArray(new float[0]);
        clear();
        return new SortState(centerArray);
    }

    public void clear() {
        quadCenters.clear();
        currentVertex = 0;
    }

    private void captureQuad() {
        // The four positions in vertexPositions form a quad. Find its center
        float totalX = 0, totalY = 0, totalZ = 0;
        for (Vector3f vertex : vertexPositions) {
            totalX += vertex.x;
            totalY += vertex.y;
            totalZ += vertex.z;
        }
        var centers = quadCenters;
        centers.ensureCapacity(centers.size() + 3);
        centers.add(totalX / 4);
        centers.add(totalY / 4);
        centers.add(totalZ / 4);
    }

    public void capture(ChunkVertexEncoder.Vertex vertex) {
        int i = currentVertex;
        vertexPositions[i].set(vertex.x, vertex.y, vertex.z);
        i++;
        if(i == 4) {
            captureQuad();
            i = 0;
        }
        currentVertex = i;
    }
}
