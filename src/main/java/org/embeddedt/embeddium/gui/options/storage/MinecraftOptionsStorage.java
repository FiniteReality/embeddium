package org.embeddedt.embeddium.gui.options.storage;

import org.embeddedt.embeddium.Embeddium;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;

public class MinecraftOptionsStorage implements OptionStorage<Options> {
    private final Minecraft client;

    public MinecraftOptionsStorage() {
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
