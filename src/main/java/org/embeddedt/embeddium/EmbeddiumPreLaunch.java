package org.embeddedt.embeddium;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.loading.FMLLoader;
import org.embeddedt.embeddium.compatibility.checks.EarlyDriverScanner;
import org.embeddedt.embeddium.compatibility.environment.probe.GraphicsAdapterProbe;
import org.embeddedt.embeddium.compatibility.workarounds.Workarounds;

public class EmbeddiumPreLaunch {
    public static void onPreLaunch() {
        if(FMLLoader.getDist() == Dist.CLIENT) {
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}