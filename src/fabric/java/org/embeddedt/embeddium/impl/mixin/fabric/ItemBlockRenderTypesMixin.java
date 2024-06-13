package org.embeddedt.embeddium.impl.mixin.fabric;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(value = ItemBlockRenderTypes.class, priority = 100)
public abstract class ItemBlockRenderTypesMixin {
    private static ChunkRenderTypeSet getRenderLayers(BlockState state) {
        return ChunkRenderTypeSet.of(ItemBlockRenderTypes.getChunkRenderType(state));
    }
}
