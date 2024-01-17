package me.jellysquid.mods.sodium.client.world.cloned;

import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelDataManager;
import org.embeddedt.embeddium.api.MeshAppender;

import java.util.Collections;
import java.util.List;

public class ChunkRenderContext {
    private final SectionPos origin;
    private final ClonedChunkSection[] sections;
    private final BoundingBox volume;
    private ModelDataManager.Snapshot modelDataSnapshot = null;
    private List<MeshAppender> meshAppenders = Collections.emptyList();

    public ChunkRenderContext(SectionPos origin, ClonedChunkSection[] sections, BoundingBox volume) {
        this.origin = origin;
        this.sections = sections;
        this.volume = volume;
    }

    public ChunkRenderContext withMeshAppenders(List<MeshAppender> meshAppenders) {
        this.meshAppenders = meshAppenders;
        return this;
    }

    public ChunkRenderContext withModelDataSnapshot(ModelDataManager.Snapshot snapshot) {
        this.modelDataSnapshot = snapshot;
        return this;
    }

    public ClonedChunkSection[] getSections() {
        return this.sections;
    }

    public SectionPos getOrigin() {
        return this.origin;
    }

    public BoundingBox getVolume() {
        return this.volume;
    }

    public List<MeshAppender> getMeshAppenders() {
        return this.meshAppenders;
    }

    public ModelDataManager.Snapshot getModelDataSnapshot() {
        return this.modelDataSnapshot;
    }
}
