package net.neoforged.fml.common;

import net.neoforged.api.distmarker.Dist;

public @interface EventBusSubscriber {
    String modid();
    Dist value();
    Bus bus();

    enum Bus {
        GAME,
        MOD
    }
}
