package org.embeddedt.embeddium.api.vertex.format.common;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.LightAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.PositionAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;
import org.embeddedt.embeddium.api.vertex.attributes.common.TextureAttribute;

public final class ParticleVertex {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.instance()
            .get(DefaultVertexFormat.PARTICLE);

    public static final int STRIDE = 28;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_TEXTURE = 12;
    private static final int OFFSET_COLOR = 20;
    private static final int OFFSET_LIGHT = 24;

    public static void put(long ptr,
                           float x, float y, float z, float u, float v, int color, int light) {
        PositionAttribute.put(ptr + OFFSET_POSITION, x, y, z);
        TextureAttribute.put(ptr + OFFSET_TEXTURE, u, v);
        ColorAttribute.set(ptr + OFFSET_COLOR, color);
        LightAttribute.set(ptr + OFFSET_LIGHT, light);
    }
}
