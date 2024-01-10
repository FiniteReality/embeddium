package org.embeddedt.embeddium.tags;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.fluid.Fluid;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

public class EmbeddiumTags {
    public static final TagKey<Fluid> RENDERS_WITH_VANILLA = TagKey.of(RegistryKeys.FLUID, new Identifier(SodiumClientMod.MODID, "is_vanilla_rendered_fluid"));
}
