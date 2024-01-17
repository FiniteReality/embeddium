package me.jellysquid.mods.sodium.client.render.vertex;

import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexFormatElement;
import it.unimi.dsi.fastutil.ints.IntList;
import it.unimi.dsi.fastutil.ints.IntLists;
import me.jellysquid.mods.sodium.client.render.vertex.transform.CommonVertexElement;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Deprecated
public class VertexFormatDescription {
    private final VertexFormat format;

    public final int id;
    public final int stride;

    public final int elementCount;
    public final int[] elementOffsets;

    public VertexFormatDescription(VertexFormat format, int id) {
        this.format = format;
        this.id = id;
        this.stride = format.getVertexSize();

        this.elementCount = format.getElements().size();
        this.elementOffsets = CommonVertexElement.getOffsets(format);
    }

    @SuppressWarnings("deprecation")
    public VertexFormatDescription(VertexFormatDescriptionImpl dsc) {
        this(dsc.format(), dsc.id());
    }

    public List<VertexFormatElement> getElements() {
        return this.format.getElements();
    }

    public IntList getOffsets() {
        return IntLists.unmodifiable(this.format.offsets);
    }

    public int getOffset(CommonVertexElement element) {
        int offset = this.elementOffsets[element.ordinal()];

        if (offset == -1) {
            throw new NoSuchElementException("Vertex format does not contain element: " + element);
        }

        return offset;
    }

    @Override
    public String toString() {
        return this.getElements()
                .stream()
                .map(e -> String.format("[%s]", e))
                .collect(Collectors.joining(","));
    }

    private static final IdentityHashMap<net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription, VertexFormatDescription> DESCRIPTION_MAP = new IdentityHashMap<>();

    public static VertexFormatDescription translateModern(net.caffeinemc.mods.sodium.api.vertex.format.VertexFormatDescription dsc) {
        if(dsc instanceof VertexFormatDescriptionImpl impl) {
            synchronized (DESCRIPTION_MAP) {
                VertexFormatDescription legacyDsc = DESCRIPTION_MAP.get(impl);
                if(legacyDsc == null) {
                    legacyDsc = new VertexFormatDescription(impl);
                    DESCRIPTION_MAP.put(impl, legacyDsc);
                }
                return legacyDsc;
            }
        }
        throw new UnsupportedOperationException("Cannot translate this type of VertexFormatDescription: " + dsc);
    }
}
