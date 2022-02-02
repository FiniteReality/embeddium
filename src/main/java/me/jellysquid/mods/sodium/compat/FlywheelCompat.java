package me.jellysquid.mods.sodium.compat;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderDispatcher;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;

import me.jellysquid.mods.sodium.SodiumClientMod;
import net.minecraft.block.entity.BlockEntity;


public class FlywheelCompat {

	public static boolean addAndFilterBEs(BlockEntity be) {
		if(SodiumClientMod.flywheelLoaded) {
		if (Backend.canUseInstancing(be.getWorld())) {
			if (InstancedRenderRegistry.canInstance(be.getType()))
				InstancedRenderDispatcher.getBlockEntities(be.getWorld()).queueAdd(be);

			if (InstancedRenderRegistry.shouldSkipRender(be))
				return false;
		}
		return true;
	}else
		return true;
	}
	
}