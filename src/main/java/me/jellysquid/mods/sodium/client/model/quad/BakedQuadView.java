package me.jellysquid.mods.sodium.client.model.quad;

import me.jellysquid.mods.sodium.client.model.quad.properties.ModelQuadFacing;
import net.minecraft.core.Direction;

public interface BakedQuadView extends ModelQuadView {
    ModelQuadFacing getNormalFace();

    Direction getLightFace();

    boolean hasShade();
}
