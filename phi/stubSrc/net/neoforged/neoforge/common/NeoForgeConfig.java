package net.neoforged.neoforge.common;

import java.util.function.Supplier;

public class NeoForgeConfig {
    public static final ClientConfig CLIENT = new ClientConfig();
    public static class ClientConfig {
        public final Supplier<Boolean> experimentalForgeLightPipelineEnabled = () -> false;
    }
}
