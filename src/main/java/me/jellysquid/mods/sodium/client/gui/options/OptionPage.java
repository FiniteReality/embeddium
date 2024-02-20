package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.client.gui.EmbeddiumOptionsAPI;

import java.util.ArrayList;
import java.util.List;

public class OptionPage {
    public static final ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");

    public static final ResourceLocation GENERAL = new ResourceLocation(SodiumClientMod.MODID, "general");
    public static final ResourceLocation QUALITY = new ResourceLocation(SodiumClientMod.MODID, "quality");
    public static final ResourceLocation PERFORMANCE = new ResourceLocation(SodiumClientMod.MODID, "performance");
    public static final ResourceLocation ADVANCED = new ResourceLocation(SodiumClientMod.MODID, "advanced");

    private final ResourceLocation id;
    private final Component name;
    private final ImmutableList<OptionGroup> groups;
    private final ImmutableList<Option<?>> options;

    @Deprecated
    public OptionPage(Component name, ImmutableList<OptionGroup> groups) {
        this(OptionPage.DEFAULT_ID, name, groups);
        SodiumClientMod.logger().warn("Id must be specified in OptionPage '{}'", name.getString());
    }

    public OptionPage(ResourceLocation id, Component name, ImmutableList<OptionGroup> groups) {
        this.id = id;
        this.name = name;

        List<Option<?>> optionsBuilder = new ArrayList<>();
        List<OptionGroup> groupsBuilder = new ArrayList<>();

        for (int i = 0; i < groups.size(); i++) {
            final OptionGroup group = groups.get(i);

            if (group.getOptions().isEmpty()) {
                continue; // ignore empty groups
            }

            if (!EmbeddiumOptionsAPI.consume(groupsBuilder, EmbeddiumOptionsAPI.customOptionGroups, group)) {
                groupsBuilder.add(group);
                optionsBuilder.addAll(group.getOptions());
            } else {
                if (groupsBuilder.get(i) == group) { // check if was added and add options too.
                    optionsBuilder.addAll(group.getOptions());
                }
            }
        }

        EmbeddiumOptionsAPI.consume(groupsBuilder, EmbeddiumOptionsAPI.customOptionGroups);

        this.groups = ImmutableList.copyOf(groupsBuilder);
        this.options = ImmutableList.copyOf(optionsBuilder);
    }

    public ResourceLocation getId() {
        return id;
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
