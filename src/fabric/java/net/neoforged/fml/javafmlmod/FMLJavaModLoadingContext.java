package net.neoforged.fml.javafmlmod;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;

public class FMLJavaModLoadingContext {
    private static final FMLJavaModLoadingContext INSTANCE = new FMLJavaModLoadingContext();

    public static FMLJavaModLoadingContext get() {
        return INSTANCE;
    }

    public static IEventBus getModEventBus() {
        return NeoForge.EVENT_BUS;
    }
}
