package org.embeddedt.embeddium.impl.gui.options;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.fml.ModList;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.storage.MinecraftOptionsStorage;
import org.embeddedt.embeddium.api.options.structure.OptionImpl;
import org.embeddedt.embeddium.api.options.structure.StandardOptions;

import java.util.Optional;

/**
 * Helper class to avoid breaking lambda offsets in main option pages class.
 */
public class FullscreenResolutionHelper {
    public static boolean isFullscreenResAlreadyAdded() {
        return ModList.get().isLoaded("embeddium_extra") || ModList.get().isLoaded("rubidium_extra");
    }

    public static OptionImpl<?, ?> createFullScreenResolutionOption() {
        Window window = Minecraft.getInstance().getWindow();
        Monitor monitor = window.findBestMonitor();
        int maxMode;
        if (monitor != null) {
            maxMode = monitor.getModeCount() - 1;
        } else {
            maxMode = -1;
        }
        ControlValueFormatter formatter = value -> {
            if (monitor == null) {
                return Component.translatable("options.fullscreen.unavailable");
            } else if (value == -1) {
                return Component.translatable("options.fullscreen.current");
            } else {
                return Component.literal(monitor.getMode(value).toString());
            }
        };
        return OptionImpl.createBuilder(int.class, MinecraftOptionsStorage.INSTANCE)
                .setId(StandardOptions.Option.FULLSCREEN_RESOLUTION)
                .setName(Component.translatable("options.fullscreen.resolution"))
                .setTooltip(Component.translatable("embeddium.options.fullscreen.resolution.tooltip"))
                .setControl(option -> new SliderControl(option, -1, maxMode, 1, formatter))
                .setBinding((opts, value) -> {
                    if (monitor != null) {
                        window.setPreferredFullscreenVideoMode(value == -1 ? Optional.empty() : Optional.of(monitor.getMode(value)));
                        window.changeFullscreenVideoMode();
                    }
                }, (opts) -> monitor != null ? window.getPreferredFullscreenVideoMode().map(monitor::getVideoModeIndex).orElse(-1) : -1)
                .build();
    }
}
