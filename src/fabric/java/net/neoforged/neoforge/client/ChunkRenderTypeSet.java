package net.neoforged.neoforge.client;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArraySet;
import net.minecraft.Util;
import net.minecraft.client.renderer.RenderType;

import java.util.Collection;
import java.util.Map;

public class ChunkRenderTypeSet extends ReferenceArraySet<RenderType> {
    private static final ChunkRenderTypeSet EMPTY = new ChunkRenderTypeSet();
    private static final ChunkRenderTypeSet ALL = new ChunkRenderTypeSet(RenderType.chunkBufferLayers());

    private ChunkRenderTypeSet() {
        super();
    }

    private ChunkRenderTypeSet(RenderType layer) {
        super(1);
        add(layer);
    }

    private ChunkRenderTypeSet(Collection<RenderType> layers) {
        super(layers.size());
        addAll(layers);
    }

    private static final Map<RenderType, ChunkRenderTypeSet> SETS = Util.make(() -> {
        Reference2ReferenceOpenHashMap<RenderType, ChunkRenderTypeSet> map = new Reference2ReferenceOpenHashMap<>(5, Reference2ReferenceOpenHashMap.VERY_FAST_LOAD_FACTOR);
        for(RenderType layer : RenderType.chunkBufferLayers()) {
            map.put(layer, new ChunkRenderTypeSet(layer));
        }
        return map;
    });

    public static ChunkRenderTypeSet of(RenderType type) {
        return SETS.get(type);
    }

    public static ChunkRenderTypeSet none() {
        return EMPTY;
    }

    public static ChunkRenderTypeSet all() {
        return ALL;
    }

    public static ChunkRenderTypeSet union(ChunkRenderTypeSet... sets) {
        ChunkRenderTypeSet set = new ChunkRenderTypeSet();
        for(ChunkRenderTypeSet other : sets) {
            set.addAll(other);
        }
        return set;
    }
}
