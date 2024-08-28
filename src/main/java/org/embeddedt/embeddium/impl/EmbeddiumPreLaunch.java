package org.embeddedt.embeddium.impl;

import org.embeddedt.embeddium.impl.compatibility.checks.EarlyDriverScanner;
import org.embeddedt.embeddium.impl.compatibility.environment.probe.GraphicsAdapterProbe;
import org.embeddedt.embeddium.impl.compatibility.workarounds.Workarounds;
import org.spongepowered.asm.mixin.MixinEnvironment;

public class EmbeddiumPreLaunch {
    public static void onPreLaunch() {
        if(MixinEnvironment.getCurrentEnvironment().getSide() == MixinEnvironment.Side.CLIENT) {
            GraphicsAdapterProbe.findAdapters();
            EarlyDriverScanner.scanDrivers();
            Workarounds.init();
        }
    }
}