package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.resources.model.BakedModel;
import org.embeddedt.embeddium.fabric.injectors.BakedModelInjector;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BakedModel.class)
public interface BakedModelMixin extends BakedModelInjector {
}
