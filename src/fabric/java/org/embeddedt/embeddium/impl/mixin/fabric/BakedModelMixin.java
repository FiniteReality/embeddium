package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.resources.model.BakedModel;
import net.neoforged.neoforge.client.extensions.IBakedModelExtension;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(BakedModel.class)
public interface BakedModelMixin extends IBakedModelExtension {
}
