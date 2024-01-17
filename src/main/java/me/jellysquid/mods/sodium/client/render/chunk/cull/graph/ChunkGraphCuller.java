package me.jellysquid.mods.sodium.client.render.chunk.cull.graph;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import me.jellysquid.mods.sodium.client.render.chunk.cull.ChunkCuller;
import me.jellysquid.mods.sodium.client.util.math.FrustumExtended;
import me.jellysquid.mods.sodium.common.util.DirectionUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.chunk.VisibilitySet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ChunkGraphCuller implements ChunkCuller {
    private final Long2ObjectMap<ChunkGraphNode> nodes = new Long2ObjectOpenHashMap<>();

    private final ChunkGraphIterationQueue visible = new ChunkGraphIterationQueue();
    private final Level world;
    private final int renderDistance;

    private FrustumExtended frustum;
    private boolean useOcclusionCulling;

    private int activeFrame = 0;
    private int centerChunkX, centerChunkY, centerChunkZ;

    public ChunkGraphCuller(Level world, int renderDistance) {
        this.world = world;
        this.renderDistance = renderDistance;
    }

    @Override
    public IntArrayList computeVisible(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.initSearch(camera, frustum, frame, spectator);

        ChunkGraphIterationQueue queue = this.visible;

        for (int i = 0; i < queue.size(); i++) {
            ChunkGraphNode node = queue.getNode(i);
            short cullData = node.computeQueuePop();

            for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
                if (useOcclusionCulling && (cullData & (1 << dir.ordinal())) == 0) {
                    continue;
                }

                ChunkGraphNode adj = node.getConnectedNode(dir);

                if (adj != null && this.isWithinRenderDistance(adj)) {
                    this.bfsEnqueue(node, adj, dir.getOpposite(), cullData);
                }
            }
        }

        return this.visible.getOrderedIdList();
    }

    private boolean isWithinRenderDistance(ChunkGraphNode adj) {
        int x = Math.abs(adj.getChunkX() - this.centerChunkX);
        int z = Math.abs(adj.getChunkZ() - this.centerChunkZ);

        return x <= this.renderDistance && z <= this.renderDistance;
    }

    private void initSearch(Camera camera, FrustumExtended frustum, int frame, boolean spectator) {
        this.activeFrame = frame;
        this.frustum = frustum;
        this.useOcclusionCulling = Minecraft.getInstance().smartCull;

        this.visible.clear();

        BlockPos origin = camera.getBlockPosition();

        int chunkX = origin.getX() >> 4;
        int chunkY = origin.getY() >> 4;
        int chunkZ = origin.getZ() >> 4;

        this.centerChunkX = chunkX;
        this.centerChunkY = chunkY;
        this.centerChunkZ = chunkZ;

        ChunkGraphNode rootNode = this.getNode(chunkX, chunkY, chunkZ);

        if (rootNode != null) {
            rootNode.resetCullingState();
            rootNode.setLastVisibleFrame(frame);

            if (spectator && this.world.getBlockState(origin).isSolidRender(this.world, origin)) {
                this.useOcclusionCulling = false;
            }

            this.visible.add(rootNode);
        } else {
            chunkY = Mth.clamp(origin.getY() >> 4, 0, 15);

            List<ChunkGraphNode> bestNodes = new ArrayList<>();

            for (int x2 = -this.renderDistance; x2 <= this.renderDistance; ++x2) {
                for (int z2 = -this.renderDistance; z2 <= this.renderDistance; ++z2) {
                    ChunkGraphNode node = this.getNode(chunkX + x2, chunkY, chunkZ + z2);

                    if (node == null || node.isCulledByFrustum(frustum)) {
                        continue;
                    }

                    node.resetCullingState();
                    node.setLastVisibleFrame(frame);

                    bestNodes.add(node);
                }
            }

            bestNodes.sort(Comparator.comparingDouble(node -> node.getSquaredDistance(origin)));

            for (ChunkGraphNode node : bestNodes) {
                this.visible.add(node);
            }
        }
    }


    private void bfsEnqueue(ChunkGraphNode parent, ChunkGraphNode node, Direction flow, short parentalData) {
        if (node.getLastVisibleFrame() == this.activeFrame) {
            node.updateCullingState(flow, parentalData);
            return;
        }
        node.setLastVisibleFrame(this.activeFrame);

        if (node.isCulledByFrustum(this.frustum)) {
            return;
        }

        node.setCullingState(parentalData);
        node.updateCullingState(flow, parentalData);

        this.visible.add(node);
    }

    private void connectNeighborNodes(ChunkGraphNode node) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkGraphNode adj = this.findAdjacentNode(node, dir);

            if (adj != null) {
                adj.setAdjacentNode(dir.getOpposite(), node);
            }

            node.setAdjacentNode(dir, adj);
        }
    }

    private void disconnectNeighborNodes(ChunkGraphNode node) {
        for (Direction dir : DirectionUtil.ALL_DIRECTIONS) {
            ChunkGraphNode adj = node.getConnectedNode(dir);

            if (adj != null) {
                adj.setAdjacentNode(dir.getOpposite(), null);
            }

            node.setAdjacentNode(dir, null);
        }
    }

    private ChunkGraphNode findAdjacentNode(ChunkGraphNode node, Direction dir) {
        return this.getNode(node.getChunkX() + dir.getStepX(), node.getChunkY() + dir.getStepY(), node.getChunkZ() + dir.getStepZ());
    }

    private ChunkGraphNode getNode(int x, int y, int z) {
        return this.nodes.get(SectionPos.asLong(x, y, z));
    }

    @Override
    public void onSectionStateChanged(int x, int y, int z, VisibilitySet occlusionData) {
        ChunkGraphNode node = this.getNode(x, y, z);

        if (node != null) {
            node.setOcclusionData(occlusionData);
        }
    }

    @Override
    public void onSectionLoaded(int x, int y, int z, int id) {
        ChunkGraphNode node = new ChunkGraphNode(x, y, z, id);
        ChunkGraphNode prev;

        if ((prev = this.nodes.put(SectionPos.asLong(x, y, z), node)) != null) {
            this.disconnectNeighborNodes(prev);
        }

        this.connectNeighborNodes(node);
    }

    @Override
    public void onSectionUnloaded(int x, int y, int z) {
        ChunkGraphNode node = this.nodes.remove(SectionPos.asLong(x, y, z));

        if (node != null) {
            this.disconnectNeighborNodes(node);
        }
    }

    @Override
    public boolean isSectionVisible(int x, int y, int z) {
        ChunkGraphNode render = this.getNode(x, y, z);

        if (render == null) {
            return false;
        }

        return render.getLastVisibleFrame() == this.activeFrame;
    }
}
