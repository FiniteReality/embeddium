package net.neoforged.neoforge.common.extensions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.ChunkPos;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.common.world.AuxiliaryLightManager;
import org.jetbrains.annotations.Nullable;

public interface IBlockAndTintGetterExtension {
    default float getShade(float normalX, float normalY, float normalZ, boolean shade) {
        return ((BlockAndTintGetter)this).getShade(Direction.getApproximateNearest(normalX, normalY, normalX), shade);
    }

    default ModelData getModelData(BlockPos pos) {
        return ModelData.EMPTY;
    }

    default @Nullable AuxiliaryLightManager getAuxLightManager(ChunkPos pos) {
        return null;
    }
}
