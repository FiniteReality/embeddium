package me.jellysquid.mods.sodium.client.model.color;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceSet;
import me.jellysquid.mods.sodium.client.model.color.interop.BlockColorsExtended;
import net.minecraft.client.color.block.BlockColors;
import net.minecraft.client.renderer.BiomeColors;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;

/**
 * The color provider registry holds the map of {@link ColorProvider}s that are currently in use by the renderer. Most
 * are adapters for a vanilla/modded BlockColor implementation. However, certain vanilla BlockColors are detected
 * and replaced to enable per-vertex biome blending.
 */
public class ColorProviderRegistry {
    private final Reference2ReferenceMap<Block, ColorProvider<BlockState>> blocks = new Reference2ReferenceOpenHashMap<>();
    private final Reference2ReferenceMap<Fluid, ColorProvider<FluidState>> fluids = new Reference2ReferenceOpenHashMap<>();

    private final ReferenceSet<Block> overridenBlocks;

    public ColorProviderRegistry(BlockColors blockColors) {
        var providers = BlockColorsExtended.getProviders(blockColors);

        for (var entry : providers.reference2ReferenceEntrySet()) {
            this.blocks.put(entry.getKey(), DefaultColorProviders.adapt(entry.getValue()));
        }

        this.overridenBlocks = BlockColorsExtended.getOverridenVanillaBlocks(blockColors);

        this.installOverrides();
    }

    // TODO: Allow mods to install their own color resolvers here
    private void installOverrides() {
        this.registerBlocks(new DefaultColorProviders.VertexBlendedBiomeColorAdapter<>(BiomeColors::getAverageGrassColor),
                Blocks.GRASS_BLOCK, Blocks.FERN, Blocks.GRASS, Blocks.POTTED_FERN,
                Blocks.SUGAR_CANE, Blocks.LARGE_FERN, Blocks.TALL_GRASS);

        this.registerBlocks(new DefaultColorProviders.VertexBlendedBiomeColorAdapter<>(BiomeColors::getAverageFoliageColor),
                Blocks.OAK_LEAVES, Blocks.JUNGLE_LEAVES, Blocks.ACACIA_LEAVES,
                Blocks.DARK_OAK_LEAVES, Blocks.VINE);

        DefaultColorProviders.VertexBlendedBiomeColorAdapter.VanillaBiomeColor waterGetter = (world, pos) -> BiomeColors.getAverageWaterColor(world, pos) | 0xFF000000;

        this.registerBlocks(new DefaultColorProviders.VertexBlendedBiomeColorAdapter<>(waterGetter),
                Blocks.WATER, Blocks.BUBBLE_COLUMN);

        this.registerFluids(new DefaultColorProviders.VertexBlendedBiomeColorAdapter<>(waterGetter),
                Fluids.WATER, Fluids.FLOWING_WATER);
    }

    private void registerBlocks(ColorProvider<BlockState> resolver, Block... blocks) {
        for (var block : blocks) {
            // Do not register our resolver if the block is overriden
            if (this.overridenBlocks.contains(block))
                continue;
            this.blocks.put(block, resolver);
        }
    }

    private void registerFluids(ColorProvider<FluidState> resolver, Fluid... fluids) {
        for (var fluid : fluids) {
            this.fluids.put(fluid, resolver);
        }
    }

    public ColorProvider<BlockState> getColorProvider(Block block) {
        return this.blocks.get(block);
    }

    public ColorProvider<FluidState> getColorProvider(Fluid fluid) {
        return this.fluids.get(fluid);
    }
}
