package me.jellysquid.mods.sodium.client.gui.options;

import com.google.common.base.Predicates;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.binding.GenericBinding;
import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;
import org.embeddedt.embeddium.client.gui.options.OptionRegistry;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

public class OptionImpl<S, T> implements Option<T> {

    private final OptionStorage<S> storage;

    private final OptionBinding<S, T> binding;
    private final Control<T> control;

    private final EnumSet<OptionFlag> flags;

    private final ResourceLocation id;
    private final Component name;
    private final Component tooltip;

    private final OptionImpact impact;

    private T value;
    private T modifiedValue;

    private final boolean enabled;

    private final ReplacementInfo<T, ?> replacementInfo;

    private final Predicate<Option<T>> visibilityPredicate;

    private boolean hasChangedPollFlag = false;

    private OptionImpl(OptionStorage<S> storage,
                       ResourceLocation id,
                       Component name,
                       Component tooltip,
                       OptionBinding<S, T> binding,
                       Function<OptionImpl<S, T>, Control<T>> control,
                       EnumSet<OptionFlag> flags,
                       OptionImpact impact,
                       boolean enabled,
                       ReplacementInfo<T, ?> replacementInfo,
                       Predicate<Option<T>> visibilityPredicate) {
        this.id = id;
        this.storage = storage;
        this.name = name;
        this.tooltip = tooltip;
        this.binding = binding;
        this.impact = impact;
        this.flags = flags;
        this.control = control.apply(this);
        this.enabled = enabled;
        this.replacementInfo = replacementInfo;
        this.visibilityPredicate = visibilityPredicate;

        this.reset();
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public Component getName() {
        return this.name;
    }

    @Override
    public Component getTooltip() {
        return this.tooltip;
    }

    @Override
    public OptionImpact getImpact() {
        return this.impact;
    }

    @Override
    public Control<T> getControl() {
        return this.control;
    }

    @Override
    public T getValue() {
        return this.modifiedValue;
    }

    @Override
    public void setValue(T value) {
        this.modifiedValue = value;
        // Delegate change to replaced option
        if(this.replacementInfo != null) {
            OptionRegistry.getOptionById(this.replacementInfo.replacedId).ifPresent(option -> ((Option)option).setValue(this.replacementInfo.valueMigrator.apply(value)));
        }
        this.hasChangedPollFlag = true;
    }

    @Override
    public void reset() {
        this.value = this.binding.getValue(this.storage.getData());
        this.modifiedValue = this.value;
        // Delegate change to replaced option
        if(this.replacementInfo != null) {
            OptionRegistry.getOptionById(this.replacementInfo.replacedId).ifPresent(Option::reset);
        }
        this.hasChangedPollFlag = false;
    }

    @Override
    public OptionStorage<?> getStorage() {
        return this.storage;
    }

    @Override
    public boolean isAvailable() {
        return this.enabled;
    }

    @Override
    public boolean hasChanged() {
        return !this.value.equals(this.modifiedValue);
    }

    @Override
    public void applyChanges() {
        this.binding.setValue(this.storage.getData(), this.modifiedValue);
        this.value = this.modifiedValue;
        // Delegate change to replaced option
        if(this.replacementInfo != null) {
            OptionRegistry.getOptionById(this.replacementInfo.replacedId).ifPresent(Option::reset);
        }
        this.hasChangedPollFlag = false;
    }

    @Override
    public Collection<OptionFlag> getFlags() {
        return this.flags;
    }

    @Override
    public boolean isVisible() {
        return this.visibilityPredicate.test(this);
    }

    @Override
    public boolean hasChangedSinceLastPoll() {
        boolean bl = this.hasChangedPollFlag;
        this.hasChangedPollFlag = false;
        return bl;
    }

    public static <S, T> OptionImpl.Builder<S, T> createBuilder(@SuppressWarnings("unused") Class<T> type, OptionStorage<S> storage) {
        return new Builder<>(storage);
    }

    private static class ReplacementInfo<T, Y> {
        ResourceLocation replacedId;
        Function<T, Y> valueMigrator;

        public ReplacementInfo(ResourceLocation oldId, Function<T, Y>  valueMigrator) {
            this.replacedId = oldId;
            this.valueMigrator = valueMigrator;
        }
    }

    public static class Builder<S, T> {
        private final OptionStorage<S> storage;
        private ResourceLocation id;
        private Component name;
        private Component tooltip;
        private OptionBinding<S, T> binding;
        private Function<OptionImpl<S, T>, Control<T>> control;
        private OptionImpact impact;
        private final EnumSet<OptionFlag> flags = EnumSet.noneOf(OptionFlag.class);
        private boolean enabled = true;
        private ReplacementInfo<T, ?> replacementInfo;
        private Predicate<Option<T>> visibilityPredicate = Predicates.alwaysTrue();

        private Builder(OptionStorage<S> storage) {
            this.storage = storage;
        }

        public Builder<S, T> setId(ResourceLocation id) {
            Validate.notNull(id, "Id must not be null");

            this.id = id;

            return this;
        }

        public Builder<S, T> setName(Component name) {
            Validate.notNull(name, "Argument must not be null");

            this.name = name;

            return this;
        }

        public Builder<S, T> setTooltip(Component tooltip) {
            Validate.notNull(tooltip, "Argument must not be null");

            this.tooltip = tooltip;

            return this;
        }

        public Builder<S, T> setBinding(BiConsumer<S, T> setter, Function<S, T> getter) {
            Validate.notNull(setter, "Setter must not be null");
            Validate.notNull(getter, "Getter must not be null");

            this.binding = new GenericBinding<>(setter, getter);

            return this;
        }


        public Builder<S, T> setBinding(OptionBinding<S, T> binding) {
            Validate.notNull(binding, "Argument must not be null");

            this.binding = binding;

            return this;
        }

        public Builder<S, T> setControl(Function<OptionImpl<S, T>, Control<T>> control) {
            Validate.notNull(control, "Argument must not be null");

            this.control = control;

            return this;
        }

        public Builder<S, T> setImpact(OptionImpact impact) {
            this.impact = impact;

            return this;
        }

        public Builder<S, T> setEnabled(boolean value) {
            this.enabled = value;

            return this;
        }

        public Builder<S, T> setFlags(OptionFlag... flags) {
            Collections.addAll(this.flags, flags);

            return this;
        }

        public Builder<S, T> setVisibilityPredicate(Predicate<Option<T>> visibilityPredicate) {
            Validate.notNull(id, "Visibility predicate must not be null");

            this.visibilityPredicate = visibilityPredicate;

            return this;
        }

        /**
         * Marks this option as replacing a built-in option.
         * <p></p>
         * When you replace an option, setValue is still called on the old option, to allow visibility predicates
         * observing it to work. However, its binding will not be used - you are responsible for replacing
         * the binding yourself.
         * @param oldId the ID of the original option
         * @param valueMigrator a migrator function that migrates the values set by this option to the same
         *                      type as that of the old option
         * @param <Y> the type of the old option
         */
        public <Y> Builder<S, T> replaces(ResourceLocation oldId, Function<T, Y> valueMigrator) {
            this.replacementInfo = new ReplacementInfo<>(oldId, valueMigrator);
            return this;
        }

        public OptionImpl<S, T> build() {
            if (this.id == null) {
                this.id = Option.DEFAULT_ID;
                SodiumClientMod.logger().warn("Id must be specified in option '{}', this might throw a exception on a future release", this.name.getString());
            }

            if (this.replacementInfo != null && Objects.equals(this.id, this.replacementInfo.replacedId)) {
                throw new IllegalArgumentException("Option must have a distinct ID from option it replaces");
            }

            if (this.name == null) {
                this.name = Component.translatable(this.id.getNamespace() + ".options." + this.id.getPath() + ".name");
            }

            if (this.tooltip == null) {
                this.tooltip = Component.translatable(this.id.getNamespace() + ".options." + this.id.getPath() + ".tooltip");
            }

            Validate.notNull(this.binding, "Option binding must be specified");
            Validate.notNull(this.control, "Control must be specified");

            OptionImpl<S, T> impl = new OptionImpl<>(this.storage, this.id, this.name, this.tooltip, this.binding, this.control,
                    this.flags, this.impact, this.enabled, this.replacementInfo, this.visibilityPredicate);
            OptionRegistry.onOptionCreate(impl);
            return impl;
        }
    }
}
