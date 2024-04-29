package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.OptionGroupConstructionEvent;
import org.embeddedt.embeddium.client.gui.options.OptionIdentifier;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {
    public static final OptionIdentifier<Void> DEFAULT_ID = OptionIdentifier.create(SodiumClientMod.MODID, "empty");

    private final ImmutableList<Option<?>> options;

    public final OptionIdentifier<Void> id;
    private OptionGroup(OptionIdentifier<Void> id, ImmutableList<Option<?>> options) {
        this.id = id;
        this.options = options;
    }

    public OptionIdentifier<Void> getId() {
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

        private OptionIdentifier<Void> id;

        public Builder setId(ResourceLocation id) {
            this.id = OptionIdentifier.create(id);
            return this;
        }

        public Builder setId(OptionIdentifier<Void> id) {
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
                // FIXME Actually enforce IDs on groups
                //SodiumClientMod.logger().warn("Id must be specified in OptionGroup which contains {}, this might throw a exception on a next release", this.options.get(0).getName().getString());
            }

            OptionGroupConstructionEvent.BUS.post(new OptionGroupConstructionEvent(this.id, this.options));

            return new OptionGroup(this.id, ImmutableList.copyOf(this.options));
        }
    }
}
