package me.jellysquid.mods.sodium.client.compat;

import java.util.Collection;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;

@Mod.EventBusSubscriber(modid = SodiumClientMod.MODID, value = Dist.CLIENT)
public class FlywheelCompat {
    private static final boolean flywheelLoaded = ModList.get().isLoaded("flywheel");

	public static boolean addAndFilterBEs(BlockEntity be) {
		if(flywheelLoaded) {
		if (Backend.canUseInstancing(be.getLevel())) {
			if (InstancedRenderRegistry.canInstance(be.getType()))
				InstancedRenderDispatcher.getBlockEntities(be.getLevel()).queueAdd(be);

			if (InstancedRenderRegistry.shouldSkipRender(be))
				return false;
		}
		return true;
	}else
		return true;
	}
	
    public static void filterBlockEntityList(Collection<BlockEntity> blockEntities) {
        if (flywheelLoaded) {
            blockEntities.removeIf(InstancedRenderRegistry::shouldSkipRender);
        }
    }

    @SubscribeEvent
    public static void onChunkDataBuilt(ChunkDataBuiltEvent event) {
        if(flywheelLoaded) {
            event.getDataBuilder().removeBlockEntitiesIf(be -> !addAndFilterBEs(be));
        }
    }
}
