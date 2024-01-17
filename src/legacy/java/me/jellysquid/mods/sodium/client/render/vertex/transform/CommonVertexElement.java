package me.jellysquid.mods.sodium.client.render.vertex.transform;

import com.google.common.collect.ImmutableList;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntList;
import java.util.Arrays;

public enum CommonVertexElement {
    POSITION,
    COLOR,
    TEXTURE,
    OVERLAY,
    LIGHT,
    NORMAL;

    public static final int COUNT = CommonVertexElement.values().length;

    public static CommonVertexElement getCommonType(VertexFormatElement element) {
        if (element == DefaultVertexFormat.ELEMENT_POSITION) {
            return POSITION;
        }

        if (element == DefaultVertexFormat.ELEMENT_COLOR) {
            return COLOR;
        }

        if (element == DefaultVertexFormat.ELEMENT_UV0) {
            return TEXTURE;
        }

        if (element == DefaultVertexFormat.ELEMENT_UV1) {
            return OVERLAY;
        }

        if (element == DefaultVertexFormat.ELEMENT_UV2) {
            return LIGHT;
        }

        if (element == DefaultVertexFormat.ELEMENT_NORMAL) {
            return NORMAL;
        }

        return null;
    }

    public static int[] getOffsets(VertexFormat format) {
        var results = new int[CommonVertexElement.COUNT];

        Arrays.fill(results, -1);

        var elements = format.getElements();
        var offsets = format.offsets;

        for (int i = 0; i < elements.size(); i++) {
            var element = elements.get(i);
            var offset = offsets.getInt(i);

            var type = CommonVertexElement.getCommonType(element);

            if (type != null) {
                results[type.ordinal()] = offset;
            }
        }

        return results;
    }
}
