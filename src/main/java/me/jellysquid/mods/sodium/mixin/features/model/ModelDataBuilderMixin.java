package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import java.util.Collections;
import java.util.Map;

@Mixin(value = ModelData.Builder.class, remap = false)
public class ModelDataBuilderMixin {
    /**
     * @author embeddedt
     * @reason IdentityHashMap.get() is slow on empty instances. Use the singleton empty map which is hardcoded to return
     * null instead.
     */
    @ModifyArg(method = "build", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/client/model/data/ModelData;<init>(Ljava/util/Map;)V"))
    private Map<ModelProperty<?>, Object> useSingletonEmptyIfPossible(Map<ModelProperty<?>, Object> properties) {
        return properties.isEmpty() ? Collections.emptyMap() : properties;
    }
}
