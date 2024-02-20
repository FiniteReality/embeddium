package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.client.gui.EmbeddiumOptionsAPI;

import java.util.ArrayList;
import java.util.List;

public class OptionGroup {
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    public static final ResourceLocation RENDERING = new ResourceLocation("minecraft", "rendering");
    public static final ResourceLocation WINDOW = new ResourceLocation("minecraft", "window");
    public static final ResourceLocation INDICATORS = new ResourceLocation("minecraft", "indicators");
    public static final ResourceLocation GRAPHICS = new ResourceLocation("minecraft", "graphics");
    public static final ResourceLocation DETAILS = new ResourceLocation("minecraft", "details");
    public static final ResourceLocation CHUNK_UPDATES = new ResourceLocation(SodiumClientMod.MODID, "chunk_updates");
    public static final ResourceLocation RENDERING_CULLING = new ResourceLocation(SodiumClientMod.MODID, "rendering_culling");
    public static final ResourceLocation CPU_SAVING = new ResourceLocation(SodiumClientMod.MODID, "cpu_saving");

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
            if (!EmbeddiumOptionsAPI.consume(options, EmbeddiumOptionsAPI.customOptions, option)) {
                this.options.add(option);
            }

            return this;
        }

        public OptionGroup build() {
            if (this.options.isEmpty()) {
                SodiumClientMod.logger().warn("OptionGroup must contains at least one option. ignoring empty group...");
            }

            if (this.id == null) {
                this.id = OptionGroup.DEFAULT_ID;
                SodiumClientMod.logger().warn("Id must be specified in OptionGroup which contains {}, this might throw a exception on a next release", this.options.get(0).getName().getString());
            }

            EmbeddiumOptionsAPI.consume(this.options, EmbeddiumOptionsAPI.customOptions);

            return new OptionGroup(this.id, ImmutableList.copyOf(this.options));
        }
    }
}
