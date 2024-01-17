package me.jellysquid.mods.sodium.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.MultipartModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;

@Mixin(MultiPartBakedModel.class)
public class MultipartBakedModelMixin {
    @Unique
    private final Map<BlockState, BakedModel[]> stateCacheFast = new Reference2ReferenceOpenHashMap<>();
    @Unique
    private final StampedLock lock = new StampedLock();

    @Shadow
    @Final
    private List<Pair<Predicate<BlockState>, BakedModel>> selectors;

    @Unique
    private BakedModel[] getModelComponents(BlockState state) {
        BakedModel[] models;

        long readStamp = this.lock.readLock();
        try {
            models = this.stateCacheFast.get(state);
        } finally {
            this.lock.unlockRead(readStamp);
        }

        if (models == null) {
            long writeStamp = this.lock.writeLock();
            try {
                List<BakedModel> modelList = new ArrayList<>(this.selectors.size());

                for (Pair<Predicate<BlockState>, BakedModel> pair : this.selectors) {
                    if (pair.getLeft().test(state)) {
                        modelList.add(pair.getRight());
                    }
                }

                models = modelList.toArray(BakedModel[]::new);
                this.stateCacheFast.put(state, models);
            } finally {
                this.lock.unlockWrite(writeStamp);
            }
        }

        return models;
    }

    /**
     * @author JellySquid
     * @reason Avoid expensive allocations and replace bitfield indirection
     */
    @Overwrite(remap = false)
    public List<BakedQuad> getQuads(BlockState state, Direction face, RandomSource random, ModelData modelData, RenderType renderLayer) {
        if (state == null) {
            return Collections.emptyList();
        }

        BakedModel[] models = getModelComponents(state);

        List<BakedQuad> quads = new ArrayList<>();
        long seed = random.nextLong();

        for (BakedModel model : models) {
            random.setSeed(seed);

            if (renderLayer == null || model.getRenderTypes(state, random, modelData).contains(renderLayer)) // FORGE: Only put quad data if the model is using the render type passed
                quads.addAll(model.getQuads(state, face, random, MultipartModelData.resolve(modelData, model), renderLayer));
        }

        return quads;
    }

    /**
     * @author embeddedt
     * @reason faster, less allocation
     */
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data) {
        BakedModel[] models = getModelComponents(state);

        long seed = random.nextLong();
        LinkedList<ChunkRenderTypeSet> renderTypeSets = new LinkedList<>();

        for (BakedModel model : models) {
            random.setSeed(seed);
            renderTypeSets.add(model.getRenderTypes(state, random, data));
        }

        return ChunkRenderTypeSet.union(renderTypeSets);
    }
}
