package org.embeddedt.embeddium.api.options.storage;

import org.embeddedt.embeddium.impl.Embeddium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.embeddedt.embeddium.api.options.structure.OptionStorage;

public class MinecraftOptionsStorage implements OptionStorage<Options> {
    public static final MinecraftOptionsStorage INSTANCE = new MinecraftOptionsStorage();

    private final Minecraft client;

    private MinecraftOptionsStorage() {
        this.client = Minecraft.getInstance();
    }

    @Override
    public Options getData() {
        return this.client.options;
    }

    @Override
    public void save() {
        this.getData().save();

        Embeddium.logger().info("Flushed changes to Minecraft configuration");
    }
}
