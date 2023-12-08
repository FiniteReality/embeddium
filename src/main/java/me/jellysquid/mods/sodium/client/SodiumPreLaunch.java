package me.jellysquid.mods.sodium.client;

import me.jellysquid.mods.sodium.client.compatibility.checks.EarlyDriverScanner;
import me.jellysquid.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import me.jellysquid.mods.sodium.client.compatibility.workarounds.Workarounds;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLLoader;

public class SodiumPreLaunch {
    public static void onPreLaunch() {
        if(FMLLoader.getDist() == Dist.CLIENT) {
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}