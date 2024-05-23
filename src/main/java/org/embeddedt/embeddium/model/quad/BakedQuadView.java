package org.embeddedt.embeddium.model.quad;

import org.embeddedt.embeddium.model.quad.properties.ModelQuadFacing;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();
    
    boolean hasShade();
}
