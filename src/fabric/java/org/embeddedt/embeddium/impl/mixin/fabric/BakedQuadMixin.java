package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.renderer.block.model.BakedQuad;
import org.embeddedt.embeddium.fabric.injectors.BakedQuadInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = BakedQuad.class, priority = 100)
public class BakedQuadMixin implements BakedQuadInjector {
    private final boolean hasAmbientOcclusion = true;
}
