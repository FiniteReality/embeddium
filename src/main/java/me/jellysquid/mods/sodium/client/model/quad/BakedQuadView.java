package me.jellysquid.mods.sodium.client.model.quad;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.core.Direction;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    boolean hasShade();

    void setFlags(int flags);
}
