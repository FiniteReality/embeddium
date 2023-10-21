package me.jellysquid.mods.sodium.client.render.chunk;

import it.unimi.dsi.fastutil.objects.*;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;
import net.minecraft.util.math.Vec3d;

import java.util.*;

public class ChunkRenderList {
    private static final Comparator<RenderRegion> REGION_DISTANCE_COMPARATOR = Comparator.comparingDouble(region -> region.distanceSquaredFromCamera);
    private final Reference2ObjectLinkedOpenHashMap<RenderRegion, List<RenderSection>> entries = new Reference2ObjectLinkedOpenHashMap<>();

    public Iterable<Map.Entry<RenderRegion, List<RenderSection>>> sorted(boolean reverse) {
        if (this.entries.isEmpty()) {
            return Collections.emptyList();
        }

        Reference2ObjectSortedMap.FastSortedEntrySet<RenderRegion, List<RenderSection>> entries =
                this.entries.reference2ObjectEntrySet();

        if (reverse) {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<RenderSection>>> iterator =
                        entries.fastIterator(entries.last());

                @Override
                public boolean hasNext() {
                    return this.iterator.hasPrevious();
                }

                @Override
                public Map.Entry<RenderRegion, List<RenderSection>> next() {
                    return this.iterator.previous();
                }
            };
        } else {
            return () -> new Iterator<>() {
                final ObjectBidirectionalIterator<Reference2ObjectMap.Entry<RenderRegion, List<RenderSection>>> iterator =
                        entries.fastIterator();

                @Override
                public boolean hasNext() {
                    return this.iterator.hasNext();
                }

                @Override
                public Map.Entry<RenderRegion, List<RenderSection>> next() {
                    return this.iterator.next();
                }
            };
        }
    }

    public void clear() {
        this.entries.clear();
    }

    public void add(RenderSection render) {
        RenderRegion region = render.getRegion();

        List<RenderSection> sections = this.entries.computeIfAbsent(region, (key) -> new ObjectArrayList<>());
        sections.add(render);
    }

    public void finalize(Vec3d cameraPos) {
        var regionList = new ObjectArrayList<>(this.entries.keySet());
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < regionList.size(); i++) {
            var region = regionList.get(i);
            double dx = cameraPos.x - region.getCenterX();
            double dy = cameraPos.y - region.getCenterY();
            double dz = cameraPos.z - region.getCenterZ();
            region.distanceSquaredFromCamera = (dx * dx) + (dy * dy) + (dz * dz);
        }
        regionList.sort(REGION_DISTANCE_COMPARATOR);
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < regionList.size(); i++) {
            var region = regionList.get(i);
            this.entries.getAndMoveToLast(region);
        }
    }

    public int getCount() {
        return this.entries.values()
                .stream()
                .mapToInt(List::size)
                .sum();
    }
}
