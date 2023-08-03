package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.util.workarounds.PreLaunchChecks;
import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import me.jellysquid.mods.sodium.client.util.workarounds.probe.GraphicsAdapterProbe;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

public class SodiumPreLaunch {
    public static void onPreLaunch() {
        if(FMLLoader.getDist() == Dist.CLIENT) {
            GraphicsAdapterProbe.findAdapters();
            PreLaunchChecks.checkDrivers();
            Workarounds.init();
        }
    }
}