package org.embeddedt.embeddium.render.fluid;

import net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderHandlerRegistry;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.material.FluidState;

public class EmbeddiumFluidSpriteCache {
    public TextureAtlasSprite[] getSprites(BlockAndTintGetter world, BlockPos pos, FluidState fluidState) {
        return FluidRenderHandlerRegistry.INSTANCE.get(fluidState.getType()).getFluidSprites(world, pos, fluidState);
    }
}
