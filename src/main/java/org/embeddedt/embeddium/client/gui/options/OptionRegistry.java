package org.embeddedt.embeddium.client.gui.options;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Maintains a mapping of ID -> option, used for various purposes within Embeddium.
 *
 * Generally, you shouldn't want to interact with this directly.
 */
@ApiStatus.Internal
public class OptionRegistry {
    private static final Map<ResourceLocation, Option<?>> OPTIONS_BY_ID = new HashMap<>();

    public static void onOptionCreate(Option<?> option) {
        OPTIONS_BY_ID.put(option.getId(), option);
    }

    public static Optional<Option<?>> getOptionById(ResourceLocation id) {
        return Optional.ofNullable(OPTIONS_BY_ID.get(id));
    }
}
