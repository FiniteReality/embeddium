package org.embeddedt.embeddium.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.client.model.ModelDataManager;
import net.minecraftforge.client.model.data.EmptyModelData;
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
    public static Map<BlockPos, IModelData> getModelDataForSection(ClientLevel world, SectionPos origin) {
        Map<BlockPos, IModelData> forgeMap = ModelDataManager.getModelData(world, origin.chunk());

        // Fast path if there is no model data in this chunk
        if(forgeMap.isEmpty())
            return Collections.emptyMap();

        Object2ObjectOpenHashMap<BlockPos, IModelData> ourMap = new Object2ObjectOpenHashMap<>();

        BoundingBox volume = new BoundingBox(origin.minBlockX(), origin.minBlockY(), origin.minBlockZ(), origin.maxBlockX(), origin.maxBlockY(), origin.maxBlockZ());

        for(Map.Entry<BlockPos, IModelData> dataEntry : forgeMap.entrySet()) {
            IModelData data = dataEntry.getValue();

            if(data == null || data == EmptyModelData.INSTANCE) {
                // There is no reason to populate the map with empty model data, because our
                // getOrDefault call will return the empty instance by default anyway
                continue;
            }

            BlockPos key = dataEntry.getKey();

            if(volume.isInside(key)) {
                ourMap.put(key, data);
            }
        }

        return ourMap.isEmpty() ? Collections.emptyMap() : ourMap;
    }
}
