package org.embeddedt.embeddium.impl.gametest.content;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.client.model.data.ModelData;
import net.minecraftforge.client.model.data.ModelProperty;
import org.jetbrains.annotations.NotNull;

public class TestBlockEntity extends BlockEntity {
    public static final ModelProperty<Object> TEST_PROPERTY = new ModelProperty<>();
    public static final Object TEST_VALUE = new Object();
    public static final ModelData TEST_DATA = ModelData.builder().with(TEST_PROPERTY, TEST_VALUE).build();

    public TestBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(TestRegistry.TEST_BLOCK_ENTITY.get(), pPos, pBlockState);
    }

    @Override
    public @NotNull ModelData getModelData() {
        return TEST_DATA;
    }
}
