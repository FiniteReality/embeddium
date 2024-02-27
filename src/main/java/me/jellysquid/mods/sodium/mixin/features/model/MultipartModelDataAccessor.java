package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.MultipartModelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import java.util.Map;

@Mixin(MultipartModelData.class)
public interface MultipartModelDataAccessor {
    @Invoker("<init>")
    static MultipartModelData create(Map<BakedModel, ModelData> partData) {
        throw new AssertionError();
    }
}
