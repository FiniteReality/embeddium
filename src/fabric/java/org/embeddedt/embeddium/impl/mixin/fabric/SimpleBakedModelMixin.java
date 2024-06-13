package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.resources.model.SimpleBakedModel;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(value = SimpleBakedModel.class, priority = 100)
public class SimpleBakedModelMixin {
    private ChunkRenderTypeSet blockRenderTypes;
}
