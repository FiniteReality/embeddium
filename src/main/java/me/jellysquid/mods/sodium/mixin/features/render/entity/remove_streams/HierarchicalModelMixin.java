package me.jellysquid.mods.sodium.mixin.features.render.entity.remove_streams;

import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import org.embeddedt.embeddium.render.entity.ModelPartExtended;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(HierarchicalModel.class)
public abstract class HierarchicalModelMixin {
    @Shadow
    public abstract ModelPart root();

    /**
     * @author embeddedt
     * @reason replace stream with cached lookup from map
     */
    @Overwrite
    public Optional<ModelPart> getAnyDescendantWithName(String pName) {
        var extendedRoot = ModelPartExtended.of(this.root());
        if(pName.equals("root")) {
            return extendedRoot.embeddium$asOptional();
        } else {
            var part = extendedRoot.embeddium$getDescendantsByName().get(pName);
            return part != null ? ModelPartExtended.of(part).embeddium$asOptional() : Optional.empty();
        }
    }
}
