package me.jellysquid.mods.sodium.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.client.resources.model.SimpleBakedModel;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.ChunkRenderTypeSet;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.MultipartModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

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
    private boolean embeddium$hasCustomRenderTypes;

    /**
     * @author embeddedt
     * @reason Forge allows the submodels to specify differing render type sets. As such, the parent model has
     * to return a union of all the render types the submodels want to render. That means the multipart's getQuads will
     * be called with render types which not all submodels may want. To solve this, Forge calls getRenderTypes again
     * inside getQuads, and suppresses the nested getQuads call for render types that aren't important. This sounds
     * good on paper, but most vanilla models will just delegate to {@link ItemBlockRenderTypes} for getRenderTypes,
     * and that is not fast as it requires two hashmap lookups. In total, each submodel will have its render types
     * queried around 8 times.
     * <p></p>
     * The solution? Vanilla multiparts will just have SimpleBakedModel instances inside, all with no render type
     * override. We can detect this and skip the unnecessary work, since in that case all the models will share
     * the same render type set, and thus not need any filtering.
     */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void checkSubModelRenderTypes(CallbackInfo ci) {
        boolean hasRenderTypes = false;
        for (var pair : selectors) {
            var model = pair.getRight();
            // Check for the exact class in case someone extends SimpleBakedModel
            if (model.getClass() == SimpleBakedModel.class) {
                // SimpleBakedModel delegates to ItemBlockRenderTypes unless there is an explicit override
                hasRenderTypes = ((SimpleBakedModelAccessor)model).getBlockRenderTypes() != null;
            } else {
                // Assume any other model needs to have getRenderTypes() called
                hasRenderTypes = true;
            }
            if (hasRenderTypes) {
                break;
            }
        }
        this.embeddium$hasCustomRenderTypes = hasRenderTypes;
    }

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

        List<BakedQuad> quads = null;
        long seed = random.nextLong();

        boolean checkSubmodelTypes = this.embeddium$hasCustomRenderTypes;

        for (BakedModel model : models) {
            random.setSeed(seed);

            // Embeddium: Filter render types as Forge does, but only if we actually need to do so. This avoids
            // the overhead of getRenderTypes() for all vanilla models.
            if (!checkSubmodelTypes || renderLayer == null || model.getRenderTypes(state, random, modelData).contains(renderLayer)) {
                List<BakedQuad> submodelQuads = model.getQuads(state, face, random, MultipartModelData.resolve(modelData, model), renderLayer);
                if(models.length == 1) {
                    // Nobody else will return quads, so no need to make a wrapper list
                    return submodelQuads;
                } else {
                    // Allocate a list to merge all the inner lists together
                    if(quads == null) {
                        quads = new ArrayList<>();
                    }
                    quads.addAll(submodelQuads);
                }
            }
        }

        return quads != null ? quads : Collections.emptyList();
    }

    /**
     * @author embeddedt
     * @reason faster, less allocation
     */
    @Overwrite(remap = false)
    public ChunkRenderTypeSet getRenderTypes(@NotNull BlockState state, @NotNull RandomSource random, @NotNull ModelData data) {
        // Consume the random value unconditionally
        long seed = random.nextLong();

        // If we know none of the submodels use custom render types, we can avoid needing to join render type sets
        // at all
        if (!this.embeddium$hasCustomRenderTypes) {
            //noinspection deprecation
            return ItemBlockRenderTypes.getRenderLayers(state);
        }

        BakedModel[] models = getModelComponents(state);

        if (models.length == 0) {
            return ChunkRenderTypeSet.none();
        }

        ChunkRenderTypeSet[] sets = new ChunkRenderTypeSet[models.length];

        for (int i = 0; i < models.length; i++) {
            random.setSeed(seed);
            sets[i] = models[i].getRenderTypes(state, random, data);
        }

        return ChunkRenderTypeSet.union(sets);
    }
}
