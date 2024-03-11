package me.jellysquid.mods.sodium.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.model.MultiPartBakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.IModelData;

import net.minecraftforge.client.model.data.MultipartModelData;
import org.apache.commons.lang3.tuple.Pair;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.*;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;

@Mixin(MultiPartBakedModel.class)
public class MixinMultipartBakedModel {
	private final Map<BlockState, BakedModel[]> stateCacheFast = new Reference2ReferenceOpenHashMap<>();
    private final StampedLock lock = new StampedLock();

    @Shadow
    @Final
    private List<Pair<Predicate<BlockState>, BakedModel>> selectors;

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

                models = modelList.toArray(new BakedModel[modelList.size()]);
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
    public List<BakedQuad> getQuads(BlockState state, Direction face, Random random, IModelData modelData) {
        if (state == null) {
            // Embeddium: There needs to be Map#get() and Map#put() calls in this method in order for FerriteCore 1.18
            // and older mixins to work. This if statement is rarely hit, and the JIT should hopefully optimize away the
            // redundant call.
            //noinspection RedundantOperationOnEmptyContainer
            if(Collections.emptyMap().get(null) != null) {
                // This must be a local so that the put() call is an interface dispatch instead of being invoked
                // on HashMap directly
                Map<Object, Object> fakeMap = new HashMap<>();
                fakeMap.put(null, null);
            }

            return Collections.emptyList();
        }

        BakedModel[] models = getModelComponents(state);

        List<BakedQuad> quads = new ArrayList<>();
        long seed = random.nextLong();

        for (BakedModel model : models) {
            random.setSeed(seed);
            quads.addAll(model.getQuads(state, face, random, MultipartModelData.resolve(model, modelData)));
        }

        return quads;
    }

    /**
     * @author embeddedt
     * @reason use our selector system, avoid creating multipart model data if no submodels use it
     */
    @Overwrite(remap = false)
    public IModelData getModelData(BlockAndTintGetter world, BlockPos pos, BlockState state, IModelData tileModelData) {
        BakedModel[] models = getModelComponents(state);

        MultipartModelData multipartModelData = null;

        for(BakedModel model : models) {
            IModelData data = model.getModelData(world, pos, state, tileModelData);
            if(data != tileModelData) {
                if(multipartModelData == null) {
                    multipartModelData = new MultipartModelData(tileModelData);
                }
                multipartModelData.setPartData(model, data);
            }
        }

        return multipartModelData == null ? tileModelData : multipartModelData;
    }

}
