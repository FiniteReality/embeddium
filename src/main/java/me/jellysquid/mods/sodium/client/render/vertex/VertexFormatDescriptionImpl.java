package me.jellysquid.mods.sodium.client.render.vertex;

import me.jellysquid.mods.sodium.mixin.core.render.VertexFormatAccessor;
import net.caffeinemc.mods.sodium.api.vertex.attributes.CommonVertexAttribute;
import net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.NoSuchElementException;

public class VertexFormatDescriptionImpl implements VertexFormatDescription {
    // legacy use only
    @Deprecated
    private final VertexFormat format;

    private final int id;
    private final int stride;

    private final int[] offsets;

    private final boolean isSimple;

    public VertexFormatDescriptionImpl(VertexFormat format, int id) {
        this.format = format;
        this.id = id;
        this.stride = format.getVertexSize();

        this.offsets = getOffsets(format);
        this.isSimple = checkSimple(format);
    }

    private static boolean checkSimple(VertexFormat format) {
        EnumSet<CommonVertexAttribute> attributeSet = EnumSet.noneOf(CommonVertexAttribute.class);
        var elementList = format.getElements();

        for (int elementIndex = 0; elementIndex < elementList.size(); elementIndex++) {
            var element = elementList.get(elementIndex);
            var commonType = CommonVertexAttribute.getCommonType(element);
            if (element.getUsage() != VertexFormatElement.Usage.PADDING && (commonType == null || !attributeSet.add(commonType))) {
                return false;
            }
        }

        return true;
    }

    public static int[] getOffsets(VertexFormat format) {
        final int[] commonElementOffsets = new int[CommonVertexAttribute.COUNT];

        Arrays.fill(commonElementOffsets, -1);

        var elementList = format.getElements();
        var elementOffsets = ((VertexFormatAccessor) format).getOffsets();

        for (int elementIndex = 0; elementIndex < elementList.size(); elementIndex++) {
            var element = elementList.get(elementIndex);
            var commonType = CommonVertexAttribute.getCommonType(element);

            if (commonType != null) {
                commonElementOffsets[commonType.ordinal()] = elementOffsets.getInt(elementIndex);
            }
        }

        return commonElementOffsets;
    }

    @Override
    public boolean containsElement(CommonVertexAttribute element) {
        return this.offsets[element.ordinal()] != -1;
    }

    @Override
    public int getElementOffset(CommonVertexAttribute element) {
        int offset = this.offsets[element.ordinal()];

        if (offset == -1) {
            throw new NoSuchElementException("Vertex format does not contain element: " + element);
        }

        return offset;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public int stride() {
        return this.stride;
    }

    @Deprecated
    public VertexFormat format() {
        return this.format;
    }

    @Override
    public boolean isSimpleFormat() {
        return this.isSimple;
    }
}
