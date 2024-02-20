package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.OptionGroupConstructionEvent;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    private final ImmutableList<Option<?>> options;

    public final ResourceLocation id;
    private OptionGroup(ResourceLocation id, ImmutableList<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public ResourceLocation getId() {
        return id;
    }

    public static Builder createBuilder() {
        return new Builder();
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public static class Builder {
        private final List<Option<?>> options = new ArrayList<>();

        private ResourceLocation id;

        public Builder setId(ResourceLocation id) {
            this.id = id;

            return this;
        }

        public Builder add(Option<?> option) {
            this.options.add(option);

            return this;
        }

        public OptionGroup build() {
            if (this.options.isEmpty()) {
                SodiumClientMod.logger().warn("OptionGroup must contain at least one option. ignoring empty group...");
            }

            if (this.id == null) {
                this.id = OptionGroup.DEFAULT_ID;
                SodiumClientMod.logger().warn("Id must be specified in OptionGroup which contains {}, this might throw a exception on a next release", this.options.get(0).getName().getString());
            }

            OptionGroupConstructionEvent.BUS.post(new OptionGroupConstructionEvent(this.id, this.options));

            return new OptionGroup(this.id, ImmutableList.copyOf(this.options));
        }
    }
}
