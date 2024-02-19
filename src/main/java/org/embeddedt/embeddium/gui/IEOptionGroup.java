package org.embeddedt.embeddium.gui;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;

public class IEOptionGroup {
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    public final ResourceLocation id;
    protected IEOptionGroup(ResourceLocation id) {
        this.id = id;
    }

    public ResourceLocation getId() {
        return id;
    }

    public static class IEBuilder {
        protected ResourceLocation id;

        public IEBuilder setId(ResourceLocation id) {
            this.id = id;

            return this;
        }
    }
}
