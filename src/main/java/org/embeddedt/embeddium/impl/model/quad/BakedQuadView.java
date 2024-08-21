package org.embeddedt.embeddium.impl.model.quad;

import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFacing;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();
    
    boolean hasShade();

    void setFlags(int flags);
}
