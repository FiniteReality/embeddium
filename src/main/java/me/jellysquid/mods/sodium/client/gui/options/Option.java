package me.jellysquid.mods.sodium.client.gui.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.options.control.Control;
import me.jellysquid.mods.sodium.client.gui.options.storage.OptionStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;

public interface Option<T>{
    ResourceLocation DEFAULT_ID = new ResourceLocation(SodiumClientMod.MODID, "empty");
    ResourceLocation RENDER_DISTANCE = new ResourceLocation("minecraft", "render_distance");
    ResourceLocation SIMULATION_DISTANCE = new ResourceLocation("minecraft", "simulation_distance");
    ResourceLocation BRIGHTNESS = new ResourceLocation("minecraft", "brightness");

    ResourceLocation GUI_SCALE = new ResourceLocation("minecraft", "gui_scale");
    ResourceLocation FULLSCREEN = new ResourceLocation("minecraft", "fullscreen");
    ResourceLocation VSYNC = new ResourceLocation("minecraft", "vsync");
    ResourceLocation MAX_FRAMERATE = new ResourceLocation("minecraft", "max_frame_rate");

    ResourceLocation VIEW_BOBBING = new ResourceLocation("minecraft", "view_bobbing");
    ResourceLocation ATTACK_INDICATOR = new ResourceLocation("minecraft", "attack_indicator");
    ResourceLocation AUTOSAVE_INDICATOR = new ResourceLocation("minecraft", "autosave_indicator");

    ResourceLocation GRAPHICS_MODE = new ResourceLocation("minecraft", "graphics_mode");
    ResourceLocation CLOUDS = new ResourceLocation("minecraft", "clouds");
    ResourceLocation WEATHER = new ResourceLocation("minecraft", "weather");
    ResourceLocation LEAVES = new ResourceLocation("minecraft", "leaves");
    ResourceLocation PARTICLES = new ResourceLocation("minecraft", "particles");
    ResourceLocation SMOOTH_LIGHT = new ResourceLocation("minecraft", "smooth_lighting");
    ResourceLocation BIOME_BLEND = new ResourceLocation("minecraft", "biome_blend");
    ResourceLocation ENTITY_DISTANCE = new ResourceLocation("minecraft", "entity_distance");
    ResourceLocation ENTITY_SHADOWS = new ResourceLocation("minecraft", "entity_shadows");
    ResourceLocation VIGNETTE = new ResourceLocation("minecraft", "vignette");
    ResourceLocation MIPMAP_LEVEL = new ResourceLocation("minecraft", "mipmap_levels");

    ResourceLocation CHUNK_UPDATE_THREADS = new ResourceLocation(SodiumClientMod.MODID, "chunk_update_threads");
    ResourceLocation DEFFER_CHUNK_UPDATES = new ResourceLocation(SodiumClientMod.MODID, "defer_chunk_updates");

    ResourceLocation BLOCK_FACE_CULLING = new ResourceLocation(SodiumClientMod.MODID, "block_face_culling");
    ResourceLocation COMPACT_VERTEX_FORMAT = new ResourceLocation(SodiumClientMod.MODID, "compact_vertex_format");
    ResourceLocation FOG_OCCLUSION = new ResourceLocation(SodiumClientMod.MODID, "fog_occlusion");
    ResourceLocation ENTITY_CULLING = new ResourceLocation(SodiumClientMod.MODID, "entity_culling");
    ResourceLocation ANIMATE_VISIBLE_TEXTURES = new ResourceLocation(SodiumClientMod.MODID, "animate_only_visible_textures");
    ResourceLocation NO_ERROR_CONTEXT = new ResourceLocation(SodiumClientMod.MODID, "no_error_context");

    ResourceLocation PERSISTENT_MAPPING = new ResourceLocation(SodiumClientMod.MODID, "persistent_mapping");
    ResourceLocation CPU_FRAMES_AHEAD = new ResourceLocation(SodiumClientMod.MODID, "cpu_render_ahead_limit");
    ResourceLocation TRANSLUCENT_FACE_SORTING = new ResourceLocation(SodiumClientMod.MODID, "translucent_face_sorting");


    default ResourceLocation getId() {
        return DEFAULT_ID;
    }

    Component getName();

    Component getTooltip();

    OptionImpact getImpact();

    Control<T> getControl();

    T getValue();

    void setValue(T value);

    void reset();

    OptionStorage<?> getStorage();

    boolean isAvailable();

    boolean hasChanged();

    void applyChanges();

    Collection<OptionFlag> getFlags();
}
