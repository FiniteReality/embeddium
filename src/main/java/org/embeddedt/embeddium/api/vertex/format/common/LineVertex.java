package org.embeddedt.embeddium.api.vertex.format.common;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import org.embeddedt.embeddium.api.vertex.attributes.common.ColorAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.NormalAttribute;
import org.embeddedt.embeddium.api.vertex.attributes.common.PositionAttribute;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatRegistry;

public final class LineVertex  {
    public static final VertexFormatDescription FORMAT = VertexFormatRegistry.instance()
            .get(DefaultVertexFormat.POSITION_COLOR_NORMAL);

    public static final int STRIDE = 20;

    private static final int OFFSET_POSITION = 0;
    private static final int OFFSET_COLOR = 12;
    private static final int OFFSET_NORMAL = 16;

    public static void put(long ptr,
                           float x, float y, float z, int color, int normal) {
        PositionAttribute.put(ptr + OFFSET_POSITION, x, y, z);
        ColorAttribute.set(ptr + OFFSET_COLOR, color);
        NormalAttribute.set(ptr + OFFSET_NORMAL, normal);
    }
}
