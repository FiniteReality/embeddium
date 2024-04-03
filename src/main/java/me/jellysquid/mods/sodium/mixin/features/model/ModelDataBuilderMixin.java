package me.jellysquid.mods.sodium.mixin.features.model;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceArrayMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Map;

@Mixin(value = ModelData.Builder.class, remap = false)
public class ModelDataBuilderMixin {
    /**
     * @author embeddedt
     * @reason Use faster backing maps for model data. For very small objects, use an array map, which gives better
     * lookup time than a hash map (due to not needing to hash). For anything larger, use a fastutil hash map,
     * which should be a bit faster than IdentityHashMap.
     */
    @Redirect(method = "build", at = @At(value = "INVOKE", target = "Ljava/util/Collections;unmodifiableMap(Ljava/util/Map;)Ljava/util/Map;"))
    private Map<ModelProperty<?>, Object> useEfficientMap(Map<ModelProperty<?>, Object> properties) {
        int size = properties.size();
        if(size >= 4) {
            return new Reference2ReferenceOpenHashMap<>(properties);
        } else if(size > 0) {
            return new Reference2ReferenceArrayMap<>(properties);
        } else {
            // this constructor uses canonical empty arrays
            return new Reference2ReferenceArrayMap<>();
        }
    }
}
