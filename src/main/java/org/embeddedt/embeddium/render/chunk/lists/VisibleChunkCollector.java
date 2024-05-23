package org.embeddedt.embeddium.render.chunk.lists;

import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.embeddedt.embeddium.render.chunk.ChunkUpdateType;
import org.embeddedt.embeddium.render.chunk.RenderSection;
import java.util.ArrayDeque;
import java.util.EnumMap;
import java.util.Map;
import java.util.Queue;
import org.embeddedt.embeddium.render.chunk.occlusion.OcclusionCuller;
import org.embeddedt.embeddium.render.chunk.region.RenderRegion;

public class VisibleChunkCollector implements OcclusionCuller.Visitor {
    private final ObjectArrayList<ChunkRenderList> sortedRenderLists;
    private final EnumMap<ChunkUpdateType, ArrayDeque<RenderSection>> sortedRebuildLists;

    private final int frame;

    public VisibleChunkCollector(int frame) {
        this.frame = frame;

        this.sortedRenderLists = new ObjectArrayList<>();
        this.sortedRebuildLists = new EnumMap<>(ChunkUpdateType.class);

        for (var type : ChunkUpdateType.values()) {
            this.sortedRebuildLists.put(type, new ArrayDeque<>());
        }
    }

    @Override
    public void visit(RenderSection section, boolean visible) {
        RenderRegion region = section.getRegion();
        ChunkRenderList renderList = region.getRenderList();

        // Even if a section does not have render objects, we must ensure the render list is initialized and put
        // into the sorted queue of lists, so that we maintain the correct order of draw calls.
        if (renderList.getLastVisibleFrame() != this.frame) {
            renderList.reset(this.frame);

            this.sortedRenderLists.add(renderList);
        }

        if (visible && section.getFlags() != 0) {
            renderList.add(section);
        }

        this.addToRebuildLists(section);
    }

    private void addToRebuildLists(RenderSection section) {
        ChunkUpdateType type = section.getPendingUpdate();

        if (type != null && section.getBuildCancellationToken() == null) {
            Queue<RenderSection> queue = this.sortedRebuildLists.get(type);

            if (queue.size() < type.getMaximumQueueSize()) {
                queue.add(section);
            }
        }
    }

    public SortedRenderLists createRenderLists() {
        return new SortedRenderLists(this.sortedRenderLists);
    }

    public Map<ChunkUpdateType, ArrayDeque<RenderSection>> getRebuildLists() {
        return this.sortedRebuildLists;
    }
}
