package org.embeddedt.embeddium.impl.gui.options;

import com.mojang.blaze3d.platform.Monitor;
import com.mojang.blaze3d.platform.Window;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.fml.ModList;
import org.embeddedt.embeddium.client.gui.options.StandardOptions;

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
        return OptionImpl.createBuilder(int.class, SodiumGameOptionPages.getVanillaOpts())
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
