package org.embeddedt.embeddium.impl.gametest.content;

import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class NotAnAirBlock extends AirBlock {
    public NotAnAirBlock() {
        super(BlockBehaviour.Properties.of().noCollission().noLootTable().air());
    }

    @Override
    public RenderShape getRenderShape(BlockState pState) {
        return RenderShape.MODEL;
    }
}
