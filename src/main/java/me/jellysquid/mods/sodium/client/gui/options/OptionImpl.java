package me.jellysquid.mods.sodium.client.gui.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.binding.GenericBinding;
import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.Validate;

import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class OptionImpl<S, T> implements Option<T> {

    public static final ResourceLocation RENDER_DISTANCE = new ResourceLocation("minecraft", "render_distance");
    public static final ResourceLocation SIMULATION_DISTANCE = new ResourceLocation("minecraft", "simulation_distance");
    public static final ResourceLocation BRIGHTNESS = new ResourceLocation("minecraft", "brightness");
    public static final ResourceLocation GUI_SCALE = new ResourceLocation("minecraft", "gui_scale");
    public static final ResourceLocation FULLSCREEN = new ResourceLocation("minecraft", "fullscreen");
    public static final ResourceLocation VSYNC = new ResourceLocation("minecraft", "vsync");
    public static final ResourceLocation MAX_FRAMERATE = new ResourceLocation("minecraft", "max_frame_rate");
    public static final ResourceLocation VIEW_BOBBING = new ResourceLocation("minecraft", "view_bobbing");
    public static final ResourceLocation ATTACK_INDICATOR = new ResourceLocation("minecraft", "attack_indicator");
    public static final ResourceLocation AUTOSAVE_INDICATOR = new ResourceLocation("minecraft", "autosave_indicator");
    public static final ResourceLocation GRAPHICS_MODE = new ResourceLocation("minecraft", "graphics_mode");
    public static final ResourceLocation CLOUDS = new ResourceLocation("minecraft", "clouds");
    public static final ResourceLocation WEATHER = new ResourceLocation("minecraft", "weather");
    public static final ResourceLocation LEAVES = new ResourceLocation("minecraft", "leaves");
    public static final ResourceLocation PARTICLES = new ResourceLocation("minecraft", "particles");
    public static final ResourceLocation SMOOTH_LIGHT = new ResourceLocation("minecraft", "smooth_lighting");
    public static final ResourceLocation BIOME_BLEND = new ResourceLocation("minecraft", "biome_blend");
    public static final ResourceLocation ENTITY_DISTANCE = new ResourceLocation("minecraft", "entity_distance");
    public static final ResourceLocation ENTITY_SHADOWS = new ResourceLocation("minecraft", "entity_shadows");
    public static final ResourceLocation VIGNETTE = new ResourceLocation("minecraft", "vignette");
    public static final ResourceLocation MIPMAP_LEVEL = new ResourceLocation("minecraft", "mipmap_levels");
    public static final ResourceLocation CHUNK_UPDATE_THREADS = new ResourceLocation(SodiumClientMod.MODID, "chunk_update_threads");
    public static final ResourceLocation DEFFER_CHUNK_UPDATES = new ResourceLocation(SodiumClientMod.MODID, "defer_chunk_updates");
    public static final ResourceLocation BLOCK_FACE_CULLING = new ResourceLocation(SodiumClientMod.MODID, "block_face_culling");
    public static final ResourceLocation COMPACT_VERTEX_FORMAT = new ResourceLocation(SodiumClientMod.MODID, "compact_vertex_format");
    public static final ResourceLocation FOG_OCCLUSION = new ResourceLocation(SodiumClientMod.MODID, "fog_occlusion");
    public static final ResourceLocation ENTITY_CULLING = new ResourceLocation(SodiumClientMod.MODID, "entity_culling");
    public static final ResourceLocation ANIMATE_VISIBLE_TEXTURES = new ResourceLocation(SodiumClientMod.MODID, "animate_only_visible_textures");
    public static final ResourceLocation NO_ERROR_CONTEXT = new ResourceLocation(SodiumClientMod.MODID, "no_error_context");
    public static final ResourceLocation PERSISTENT_MAPPING = new ResourceLocation(SodiumClientMod.MODID, "persistent_mapping");
    public static final ResourceLocation CPU_FRAMES_AHEAD = new ResourceLocation(SodiumClientMod.MODID, "cpu_render_ahead_limit");
    public static final ResourceLocation TRANSLUCENT_FACE_SORTING = new ResourceLocation(SodiumClientMod.MODID, "translucent_face_sorting");
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

    private OptionImpl(OptionStorage<S> storage,
                       ResourceLocation id,
                       Component name,
                       Component tooltip,
                       OptionBinding<S, T> binding,
                       Function<OptionImpl<S, T>, Control<T>> control,
                       EnumSet<OptionFlag> flags,
                       OptionImpact impact,
                       boolean enabled) {
        this.id = id;
        this.storage = storage;
        this.name = name;
        this.tooltip = tooltip;
        this.binding = binding;
        this.impact = impact;
        this.flags = flags;
        this.control = control.apply(this);
        this.enabled = enabled;

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
    }

    @Override
    public void reset() {
        this.value = this.binding.getValue(this.storage.getData());
        this.modifiedValue = this.value;
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
    }

    @Override
    public Collection<OptionFlag> getFlags() {
        return this.flags;
    }

    public static <S, T> OptionImpl.Builder<S, T> createBuilder(@SuppressWarnings("unused") Class<T> type, OptionStorage<S> storage) {
        return new Builder<>(storage);
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

        public OptionImpl<S, T> build() {
            Validate.notNull(this.name, "Name must be specified");
            Validate.notNull(this.tooltip, "Tooltip must be specified");
            Validate.notNull(this.binding, "Option binding must be specified");
            Validate.notNull(this.control, "Control must be specified");

            if (this.id == null) {
                this.id = Option.DEFAULT_ID;
                SodiumClientMod.logger().warn("Id must be specified in option '{}', this might throw a exception on a future release", this.name.getString());
            }

            return new OptionImpl<>(this.storage, this.id, this.name, this.tooltip, this.binding, this.control, this.flags, this.impact, this.enabled);
        }
    }
}
