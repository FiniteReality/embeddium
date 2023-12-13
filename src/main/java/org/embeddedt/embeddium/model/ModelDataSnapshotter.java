package org.embeddedt.embeddium.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.util.math.BlockBox;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.IModelData;

import java.util.Collections;
import java.util.Map;

public class ModelDataSnapshotter {
    /**
     * Retrieve all needed model data for the given subchunk.
     * @param world the client world to retrieve data for
     * @param origin the origin of the subchunk
     * @return a map of all model data contained within this subchunk
     */
    public static Map<BlockPos, IModelData> getModelDataForSection(ClientWorld world, ChunkSectionPos origin) {
        Map<BlockPos, IModelData> forgeMap = ModelDataManager.getModelData(world, origin.toChunkPos());

        // Fast path if there is no model data in this chunk
        if(forgeMap.isEmpty())
            return Collections.emptyMap();

        Object2ObjectOpenHashMap<BlockPos, IModelData> ourMap = new Object2ObjectOpenHashMap<>();

        BlockBox volume = new BlockBox(origin.getMinX(), origin.getMinY(), origin.getMinZ(), origin.getMaxX(), origin.getMaxY(), origin.getMaxZ());

        for(Map.Entry<BlockPos, IModelData> dataEntry : forgeMap.entrySet()) {
            BlockPos key = dataEntry.getKey();

            if(volume.contains(key)) {
                IModelData data = dataEntry.getValue();
                if(data != null) {
                    ourMap.put(key, data);
                }
            }
        }

        return ourMap.isEmpty() ? Collections.emptyMap() : ourMap;
    }
}
