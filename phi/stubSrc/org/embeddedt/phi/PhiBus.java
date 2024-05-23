package org.embeddedt.phi;

import net.neoforged.bus.api.BusBuilder;
import net.neoforged.bus.api.IEventBus;

public class PhiBus {
    public static final IEventBus BUS = BusBuilder.builder().build();
}
