package me.jellysquid.mods.sodium.mixin.features.model;

import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Mixin(value = ModelData.class, remap = false)
public class ModelDataMixin {
    @Shadow
    @Final
    private Map<ModelProperty<?>, Object> properties;

    private Set<ModelProperty<?>> embeddium$propertySetView;

    /**
     * @author embeddedt
     * @reason Do not expose the mutable set from fastutil to user code.
     */
    @Overwrite
    public Set<ModelProperty<?>> getProperties() {
        Set<ModelProperty<?>> view = this.embeddium$propertySetView;
        if (view == null) {
            this.embeddium$propertySetView = view = Collections.unmodifiableSet(this.properties.keySet());
        }
        return view;
    }
}
