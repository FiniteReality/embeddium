package org.embeddedt.embeddium.gui;

import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;

public class IEOptionImpl<S, T> implements IEOption<T> {
    protected final ResourceLocation id;

    protected IEOptionImpl(ResourceLocation id) {
        this.id = id;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    public static class IEBuilder {
        protected ResourceLocation id;

        public IEBuilder setId(ResourceLocation id) {
            Validate.notNull(id, "Id must not be null");

            this.id = id;

            return this;
        }
    }
}
