package me.jellysquid.mods.sodium.client.render.vanilla.block;

import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ReferenceOpenHashSet;
import java.util.Set;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

public class DefaultBlockColorSettings {
	
	private static final TagKey<Block> MODDED_BLENDED_BLOCKS = TagKey.create(Registry.BLOCK_REGISTRY, new ResourceLocation("c", "blendable_blocks"));
    private static final TagKey<Fluid> MODDED_BLENDED_FLUIDS = TagKey.create(Registry.FLUID_REGISTRY, new ResourceLocation("c", "blendable_fluids"));
	
	private static final Set<Block> BLENDED_BLOCKS = new ReferenceOpenHashSet<>(Sets.newHashSet(
            Blocks.FERN, Blocks.LARGE_FERN, Blocks.POTTED_FERN, Blocks.GRASS, Blocks.TALL_GRASS,
            Blocks.GRASS_BLOCK, Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES, Blocks.DARK_OAK_LEAVES,
            Blocks.MANGROVE_LEAVES, Blocks.AZALEA_LEAVES, Blocks.BIRCH_LEAVES, Blocks.FLOWERING_AZALEA_LEAVES,
            Blocks.VINE, Blocks.WATER, Blocks.BUBBLE_COLUMN, Blocks.WATER_CAULDRON, Blocks.LAVA_CAULDRON, Blocks.CAULDRON, Blocks.SUGAR_CANE));

    private static final Set<Fluid> BLENDED_FLUIDS = new ReferenceOpenHashSet<>(Sets.newHashSet(
            Fluids.EMPTY, Fluids.WATER, Fluids.FLOWING_WATER, Fluids.LAVA, Fluids.FLOWING_LAVA));

    /**
     * Gets a value indicating if the specified block is registered for smooth blending.
     */
    public static boolean isSmoothBlendingAvailable(Block block) {
        return BLENDED_BLOCKS.contains(block) || block.defaultBlockState().is(MODDED_BLENDED_BLOCKS);
    }
    
    /**
     * Gets a value indicating if the specified fluid is registered for smooth blending.
     */
    public static boolean isSmoothBlendingAvailable(Fluid fluid) {
        return BLENDED_FLUIDS.contains(fluid) || fluid.defaultFluidState().is(MODDED_BLENDED_FLUIDS);
    }

    /**
     * Registers a block for smooth blending.
     */
    public static void registerForBlending(Block block) {
        BLENDED_BLOCKS.add(block);
    }

    /**
     * Registers a fluid for smooth blending.
     */
    public static void registerForBlending(Fluid fluid) {
        BLENDED_FLUIDS.add(fluid);
    }
}