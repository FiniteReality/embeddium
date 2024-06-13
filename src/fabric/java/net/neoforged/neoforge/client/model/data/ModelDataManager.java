package net.neoforged.neoforge.client.model.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;

public class ModelDataManager {
    public static final ModelDataManager INSTANCE = new ModelDataManager();

    public static final Long2ObjectFunction<ModelData> EMPTY_SNAPSHOT = key -> ModelData.EMPTY;

    public Long2ObjectFunction<ModelData> snapshotSectionRegion(int x1, int y1, int z1, int x2, int y2, int z2) {
        return EMPTY_SNAPSHOT;
    }
}
