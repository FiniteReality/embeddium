package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;

public class OptionPage {
    private final Component name;
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    public OptionPage(String name, ImmutableList<OptionGroup> groups) {
        this(new TextComponent(name), groups);
    }

    public OptionPage(Component name, ImmutableList<OptionGroup> groups) {
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

    public Component getNewName() {
        return this.name;
    }

    public String getName() {
        return this.getNewName().getString();
    }

}
