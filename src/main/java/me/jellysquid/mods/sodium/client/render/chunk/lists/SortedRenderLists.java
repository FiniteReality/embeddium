package me.jellysquid.mods.sodium.client.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.jellysquid.mods.sodium.client.render.chunk.RenderSection;
import me.jellysquid.mods.sodium.client.util.iterator.ReversibleObjectArrayIterator;
import me.jellysquid.mods.sodium.client.render.chunk.region.RenderRegion;

public class SortedRenderLists implements ChunkRenderListIterable {
    private static final SortedRenderLists EMPTY = new SortedRenderLists(new ObjectArrayList<>());

    private final ObjectArrayList<ChunkRenderList> lists;

    SortedRenderLists(ObjectArrayList<ChunkRenderList> lists) {
        this.lists = lists;
    }

    @Override
    public ReversibleObjectArrayIterator<ChunkRenderList> iterator(boolean reverse) {
        return new ReversibleObjectArrayIterator<>(this.lists, reverse);
    }

    public static SortedRenderLists empty() {
        return EMPTY;
    }

    public static class Builder {
        private final ObjectArrayList<ChunkRenderList> lists = new ObjectArrayList<>();
        private final int frame;

        public Builder(int frame) {
            this.frame = frame;
        }

        public void add(RenderSection section) {
            RenderRegion region = section.getRegion();
            ChunkRenderList list = region.getRenderList();

            // Even if a section does not have render objects, we must ensure the render list is initialized and put
            // into the sorted queue of lists, so that we maintain the correct order of draw calls.
            if (list.getLastVisibleFrame() != this.frame) {
                list.reset(this.frame);

                this.lists.add(list);
            }

            // Only add the section to the render list if it actually contains render objects
            if (section.getFlags() != 0) {
                list.add(section);
            }
        }

        public SortedRenderLists build() {
            var filtered = new ObjectArrayList<ChunkRenderList>(this.lists.size());

            // Filter any empty render lists
            for (var list : this.lists) {
                if (list.size() > 0) {
                    filtered.add(list);
                }
            }

            return new SortedRenderLists(filtered);
        }
    }
}
