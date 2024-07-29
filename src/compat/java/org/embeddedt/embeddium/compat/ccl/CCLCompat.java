package org.embeddedt.embeddium.compat.ccl;

import codechicken.lib.render.block.BlockRenderingRegistry;
import codechicken.lib.render.block.ICCBlockRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.registries.IRegistryDelegate;
import org.embeddedt.embeddium.api.BlockRendererRegistry;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CCLCompat {
    private static final Logger LOGGER = LoggerFactory.getLogger("Embeddium-CCL");
	private static Map<IRegistryDelegate<Block>, ICCBlockRenderer> customBlockRenderers;
    private static Map<IRegistryDelegate<Fluid>, ICCBlockRenderer> customFluidRenderers;
    private static List<ICCBlockRenderer> customGlobalRenderers;

    private static final Map<ICCBlockRenderer, BlockRendererRegistry.Renderer> ccRendererToSodium = new ConcurrentHashMap<>();
    private static final ThreadLocal<PoseStack> STACK_THREAD_LOCAL = ThreadLocal.withInitial(PoseStack::new);

    /**
     * Wrap a CodeChickenLib renderer in Embeddium's API.
     */
    private static BlockRendererRegistry.Renderer createBridge(ICCBlockRenderer r) {
        return ccRendererToSodium.computeIfAbsent(r, ccRenderer -> (ctx, random, consumer) -> {
            ccRenderer.renderBlock(ctx.state(), ctx.pos(), ctx.world(), STACK_THREAD_LOCAL.get(), consumer, random, ctx.modelData());
            return BlockRendererRegistry.RenderResult.OVERRIDE;
        });
    }

    public static void onClientSetup(FMLClientSetupEvent event) {
        if(ModList.get().isLoaded("codechickenlib")) {
            init();
            BlockRendererRegistry.instance().registerRenderPopulator((resultList, ctx) -> {
                if(!customGlobalRenderers.isEmpty()) {
                    for(ICCBlockRenderer r : customGlobalRenderers) {
                        if(r.canHandleBlock(ctx.world(), ctx.pos(), ctx.state())) {
                            resultList.add(createBridge(r));
                        }
                    }
                }
                if(!customBlockRenderers.isEmpty()) {
                    Block block = ctx.state().getBlock();
                    for(Map.Entry<IRegistryDelegate<Block>, ICCBlockRenderer> entry : customBlockRenderers.entrySet()) {
                        if(entry.getKey().get() == block && entry.getValue().canHandleBlock(ctx.world(), ctx.pos(), ctx.state())) {
                            resultList.add(createBridge(entry.getValue()));
                        }
                    }
                }
                if(!customFluidRenderers.isEmpty()) {
                    Fluid fluid = ctx.state().getFluidState().getType();
                    for(Map.Entry<IRegistryDelegate<Fluid>, ICCBlockRenderer> entry : customFluidRenderers.entrySet()) {
                        if(entry.getKey().get().isSame(fluid) && entry.getValue().canHandleBlock(ctx.world(), ctx.pos(), ctx.state())) {
                            resultList.add(createBridge(entry.getValue()));
                        }
                    }
                }
            });
        }
    }

    
	@SuppressWarnings("unchecked")
	public static void init() {
		try {
			LOGGER.info("Retrieving block renderers");
            final Field blockRenderersField = BlockRenderingRegistry.class.getDeclaredField("blockRenderers");
            blockRenderersField.setAccessible(true);
            customBlockRenderers = (Map<IRegistryDelegate<Block>, ICCBlockRenderer>) blockRenderersField.get(null);

            LOGGER.info("Retrieving fluid renderers");
            final Field fluidRenderersField = BlockRenderingRegistry.class.getDeclaredField("fluidRenderers");
            fluidRenderersField.setAccessible(true);
            customFluidRenderers = (Map<IRegistryDelegate<Fluid>, ICCBlockRenderer>) fluidRenderersField.get(null);

            LOGGER.info("Retrieving global renderers");
            final Field globalRenderersField = BlockRenderingRegistry.class.getDeclaredField("globalRenderers");
            globalRenderersField.setAccessible(true);
            customGlobalRenderers = (List<ICCBlockRenderer>) globalRenderersField.get(null);

            if(customBlockRenderers == null)
                customBlockRenderers = Collections.emptyMap();
            if(customFluidRenderers == null)
                customFluidRenderers = Collections.emptyMap();
            if(customGlobalRenderers == null)
                customGlobalRenderers = Collections.emptyList();
        }
        catch (final @NotNull Throwable t) {
        	LOGGER.error("Could not retrieve custom renderers");
        }

	}
	
}
