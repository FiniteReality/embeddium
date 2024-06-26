package org.embeddedt.embeddium.render.entity;

import net.minecraft.client.model.geom.ModelPart;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface ModelPartExtended {
    List<ModelPart> embeddium$getPartsList();
    Optional<ModelPart> embeddium$asOptional();
    Map<String, ModelPart> embeddium$getDescendantsByName();

    static ModelPartExtended of(ModelPart part) {
        return (ModelPartExtended)(Object)part;
    }
}
