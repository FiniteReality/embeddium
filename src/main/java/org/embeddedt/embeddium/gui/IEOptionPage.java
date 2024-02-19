package org.embeddedt.embeddium.gui;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;

public class IEOptionPage {
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    private final ResourceLocation id;

    protected IEOptionPage(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }
}
