package org.embeddedt.embeddium.impl.render.immediate.model;

import net.minecraft.client.model.geom.ModelPart;

public interface ModelPartData {
    static ModelPartData from(ModelPart child) {
        return (ModelPartData) (Object) child;
    }

    @Deprecated
    ModelCuboid[] getCuboids();
    @Deprecated
    ModelPart[] getChildren();

    boolean isVisible();
    boolean isHidden();
}
