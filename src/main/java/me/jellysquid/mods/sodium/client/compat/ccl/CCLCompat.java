package me.jellysquid.mods.sodium.client.compat.ccl;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.registry.entry.RegistryEntry;
import org.jetbrains.annotations.NotNull;

import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.fluid.Fluid;
import net.minecraft.fluid.FluidState;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockRenderView;

public class CCLCompat {
	
	public static Map<RegistryEntry<Block>, ICCBlockRenderer> customBlockRenderers;
	public static Map<RegistryEntry<Fluid>, ICCBlockRenderer> customFluidRenderers;
	public static List<ICCBlockRenderer> customGlobalRenderers;

    public static @NotNull List<ICCBlockRenderer> getCustomRenderers(final @NotNull BlockRenderView world, final @NotNull BlockPos pos) {
        final BlockState state = world.getBlockState(pos);
        final Block block = state.getBlock();

        final FluidState fluidState = state.getFluidState();
        final Fluid fluid = fluidState.getFluid();

        if(customGlobalRenderers == null)
        	return new ArrayList<>();
        
        final ArrayList<ICCBlockRenderer> renderers = new ArrayList<>(customGlobalRenderers);

        if(customBlockRenderers != null)
	        for (final Map.Entry<RegistryEntry<Block>, ICCBlockRenderer> entry : customBlockRenderers.entrySet()) {
	            final Block entryBlock = entry.getKey().value();
	
	            if (entryBlock == block) {
	                renderers.add(entry.getValue());
	            }
	        }

        if(customFluidRenderers != null)
	        for (final Map.Entry<RegistryEntry<Fluid>, ICCBlockRenderer> entry : customFluidRenderers.entrySet()) {
	            final Fluid entryFluid = entry.getKey().value();
	
	            if (entryFluid.matchesType(fluid)) {
	                renderers.add(entry.getValue());
	            }
	        }

        return renderers;
    }

    
	@SuppressWarnings("unchecked")
	public static void init() {
		try {
			SodiumClientMod.logger().info("Retrieving block renderers");
            final Field blockRenderersField = BlockRenderingRegistry.class.getDeclaredField("blockRenderers");
            blockRenderersField.setAccessible(true);
            customBlockRenderers = (Map<RegistryEntry<Block>, ICCBlockRenderer>) blockRenderersField.get(null);

            SodiumClientMod.logger().info("Retrieving fluid renderers");
            final Field fluidRenderersField = BlockRenderingRegistry.class.getDeclaredField("fluidRenderers");
            fluidRenderersField.setAccessible(true);
            customFluidRenderers = (Map<RegistryEntry<Fluid>, ICCBlockRenderer>) fluidRenderersField.get(null);

            SodiumClientMod.logger().info("Retrieving global renderers");
            final Field globalRenderersField = BlockRenderingRegistry.class.getDeclaredField("globalRenderers");
            globalRenderersField.setAccessible(true);
            customGlobalRenderers = (List<ICCBlockRenderer>) globalRenderersField.get(null);
        }
        catch (final @NotNull Throwable t) {
        	SodiumClientMod.logger().error("Could not retrieve custom renderers");
        }

	}
	
}
