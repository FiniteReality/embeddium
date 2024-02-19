package org.embeddedt.embeddium.gui;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;

public interface IEOption<T> {
    ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    default ResourceLocation getId() {
        return DEFAULT_ID;
    }
}
