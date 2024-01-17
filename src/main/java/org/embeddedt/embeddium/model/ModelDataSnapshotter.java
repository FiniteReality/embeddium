package org.embeddedt.embeddium.model;

import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraftforge.client.model.data.ModelData;

import java.util.Collections;
import java.util.Map;

public class ModelDataSnapshotter {
    /**
     * Retrieve all needed model data for the given subchunk.
     * @param world the client world to retrieve data for
     * @param origin the origin of the subchunk
     * @return a map of all model data contained within this subchunk
     */
    public static Map<BlockPos, ModelData> getModelDataForSection(ClientLevel world, SectionPos origin) {
        Map<BlockPos, ModelData> forgeMap = world.getModelDataManager().getAt(origin.chunk());

        // Fast path if there is no model data in this chunk
        if(forgeMap.isEmpty())
            return Collections.emptyMap();

        Object2ObjectOpenHashMap<BlockPos, ModelData> ourMap = new Object2ObjectOpenHashMap<>();

        BoundingBox volume = new BoundingBox(origin.minBlockX(), origin.minBlockY(), origin.minBlockZ(), origin.maxBlockX(), origin.maxBlockY(), origin.maxBlockZ());

        for(Map.Entry<BlockPos, ModelData> dataEntry : forgeMap.entrySet()) {
            ModelData data = dataEntry.getValue();

            if(data == null || data == ModelData.EMPTY) {
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
