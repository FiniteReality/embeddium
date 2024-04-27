package me.jellysquid.mods.sodium.client.render.chunk.vertex.format;

import me.jellysquid.mods.sodium.client.render.chunk.terrain.material.Material;

public interface ChunkVertexEncoder {
    long write(long ptr, Material material, Vertex vertex, int sectionIndex);

    class Vertex {
        public float x;
        public float y;
        public float z;
        public int color;
        public float u;
        public float v;
        public int light;
        /**
         * The normal that vanilla would output for this quad. Unused by Embeddium's built-in shaders, but might be used
         * by a core shader.
         */
        public int normal;

        public static Vertex[] uninitializedQuad() {
            Vertex[] vertices = new Vertex[4];

            for (int i = 0; i < 4; i++) {
                vertices[i] = new Vertex();
            }

            return vertices;
        }
    }
}
