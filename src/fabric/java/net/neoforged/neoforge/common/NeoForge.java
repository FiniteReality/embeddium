package net.neoforged.neoforge.common;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;

public class NeoForge {
    public static final IEventBus EVENT_BUS = BusBuilder.builder().build();
}
