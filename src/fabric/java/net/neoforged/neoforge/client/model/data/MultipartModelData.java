package net.neoforged.neoforge.client.model.data;

import net.minecraft.client.resources.model.BakedModel;

import java.util.Map;

public class MultipartModelData {
    private static final ModelProperty<Map<BakedModel, ModelData>> PROPERTY = new ModelProperty<>();

    public static ModelData resolve(ModelData modelData, BakedModel model) {
        return modelData;
    }
}
