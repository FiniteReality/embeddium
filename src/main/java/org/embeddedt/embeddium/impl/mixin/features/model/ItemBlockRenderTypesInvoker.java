package org.embeddedt.embeddium.impl.mixin.features.model;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.ChunkRenderTypeSet;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ItemBlockRenderTypes.class)
public interface ItemBlockRenderTypesInvoker {
    @Invoker
    static ChunkRenderTypeSet invokeGetRenderLayers(BlockState state) {
        throw new AssertionError();
    }
}
