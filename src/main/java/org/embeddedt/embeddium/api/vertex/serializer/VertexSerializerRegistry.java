package org.embeddedt.embeddium.api.vertex.serializer;

import org.embeddedt.embeddium.api.internal.DependencyInjection;
import org.embeddedt.embeddium.api.vertex.format.VertexFormatDescription;

public interface VertexSerializerRegistry {
    VertexSerializerRegistry INSTANCE = DependencyInjection.load(VertexSerializerRegistry.class,
            "org.embeddedt.embeddium.impl.render.vertex.serializers.VertexSerializerRegistryImpl");

    static VertexSerializerRegistry instance() {
        return INSTANCE;
    }

    VertexSerializer get(VertexFormatDescription srcFormat, VertexFormatDescription dstFormat);
}
