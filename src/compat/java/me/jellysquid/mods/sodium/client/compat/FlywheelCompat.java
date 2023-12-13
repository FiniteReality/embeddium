package me.jellysquid.mods.sodium.client.compat;

import java.util.Collection;

import com.jozufozu.flywheel.backend.Backend;
import com.jozufozu.flywheel.backend.instancing.InstancedRenderRegistry;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.block.entity.BlockEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import org.embeddedt.embeddium.api.ChunkDataBuiltEvent;

@Mod.EventBusSubscriber(modid = SodiumClientMod.MODID, value = Dist.CLIENT)
public class FlywheelCompat {
    private static final boolean flywheelLoaded = ModList.get().isLoaded("flywheel");

    /**
     * Filters a collection of TileEntities to avoid rendering conflicts with Flywheel.
     *
     * @param blockEntities The collection to be filtered.
     */
    public static void filterBlockEntityList(Collection<BlockEntity> blockEntities) {
        if (flywheelLoaded && Backend.getInstance().canUseInstancing()) {
            InstancedRenderRegistry r = InstancedRenderRegistry.getInstance();
            blockEntities.removeIf(r::shouldSkipRender);
        }
    }

    public static boolean isSkipped(BlockEntity be) {
        if(!flywheelLoaded)
            return false;
        if(!Backend.getInstance().canUseInstancing())
            return false;
        return InstancedRenderRegistry.getInstance().shouldSkipRender(be);
    }

    @SubscribeEvent
    public static void onChunkDataBuilt(ChunkDataBuiltEvent event) {
        if(flywheelLoaded) {
            event.getDataBuilder().removeBlockEntitiesIf(FlywheelCompat::isSkipped);
        }
    }
}