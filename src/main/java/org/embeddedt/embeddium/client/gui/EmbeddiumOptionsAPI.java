package org.embeddedt.embeddium.client.gui;

import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;

import java.util.ArrayList;
import java.util.List;

public class EmbeddiumOptionsAPI {
    public static final List<CustomOption<OptionPage>> customPages = new ArrayList<>();
    public static final List<CustomOption<OptionGroup>> customOptionGroups = new ArrayList<>();
    public static final List<CustomOption<Option<?>>> customOptions = new ArrayList<>();

    public static void addCustomPage(CustomOption<OptionPage> custom) {
        customPages.add(custom);
    }

    public static void addCustomOptionGroup(CustomOption<OptionGroup> custom) {
        customOptionGroups.add(custom);
    }

    public static void addCustomOption(CustomOption<Option<?>> custom) {
        customOptions.add(custom);
    }

    /**
     * Consumes the option and injects customizations
     * @param optionsList options source
     * @param customOptionsList custom options source
     * @param option option to consume
     * @return true if consume already decides what do with the option, false if there isn't any decision taken.
     * @param <T> option type
     */
    public static <T> boolean consume(List<T> optionsList, List<CustomOption<T>> customOptionsList, T option) {
        for (CustomOption<T> customOpt: customOptionsList) {
            if (!customOpt.shouldApply(option)) continue;

            switch (customOpt.mode()) {
                case REPLACE: {
                    optionsList.add(customOpt.getOption());
                    return true;
                }

                case ADD_BEFORE: {
                    optionsList.add(customOpt.getOption());
                    optionsList.add(option);
                    return true;
                }

                case ADD_AFTER: {
                    optionsList.add(option);
                    optionsList.add(customOpt.getOption());
                    return true;
                }

                case DELETE: {
                    return true;
                }
            }
        }

        return false;
    }

    public static <T> boolean consume(List<T> optionsList, List<CustomOption<T>> customOptionsList) {
        for (CustomOption<T> customOpt: customOptionsList) {
            switch (customOpt.mode()) {
                case HEAD: {
                    optionsList.add(0, customOpt.getOption());
                    return true;
                }

                case TAIL: {
                    optionsList.add(customOpt.getOption());
                    return true;
                }
            }
        }
        return false;
    }
}
