package org.embeddedt.embeddium.api.vertex.format;

import com.mojang.blaze3d.vertex.VertexFormat;
import org.embeddedt.embeddium.api.internal.DependencyInjection;

public interface VertexFormatRegistry {
    VertexFormatRegistry INSTANCE = DependencyInjection.load(VertexFormatRegistry.class,
            "org.embeddedt.embeddium.impl.render.vertex.VertexFormatRegistryImpl");

    static VertexFormatRegistry instance() {
        return INSTANCE;
    }

    VertexFormatDescription get(VertexFormat format);
}