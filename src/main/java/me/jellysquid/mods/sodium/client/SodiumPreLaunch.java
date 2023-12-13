package me.jellysquid.mods.sodium.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import me.jellysquid.mods.sodium.client.compatibility.checks.EarlyDriverScanner;
import me.jellysquid.mods.sodium.client.compatibility.environment.probe.GraphicsAdapterProbe;
import me.jellysquid.mods.sodium.client.compatibility.workarounds.Workarounds;

public class SodiumPreLaunch {
    public static void onPreLaunch() {
        if(FMLLoader.getDist() == Dist.CLIENT) {
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}