package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.gui.IEOptionPage;

public class OptionPage extends IEOptionPage {
    private final Component name;
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    @Deprecated
    public OptionPage(Component name, ImmutableList<OptionGroup> groups) {
        this(OptionPage.DEFAULT_ID, name, groups);
        SodiumClientMod.logger().warn("Id must be specified in OptionPage '{}'", name.getString());
    }

    public OptionPage(ResourceLocation id, Component name, ImmutableList<OptionGroup> groups) {
        super(id);
        this.name = name;
        this.groups = groups;

        ImmutableList.Builder<Option<?>> builder = ImmutableList.builder();

        for (OptionGroup group : groups) {
            builder.addAll(group.getOptions());
        }

        this.options = builder.build();
    }

    public ImmutableList<OptionGroup> getGroups() {
        return this.groups;
    }

    public ImmutableList<Option<?>> getOptions() {
        return this.options;
    }

    public Component getName() {
        return this.name;
    }

}
