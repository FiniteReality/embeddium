package org.embeddedt.embeddium.client.gui.options;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraft.resources.ResourceLocation;

public final class StandardOptions {
    public static class Group {
        public static final ResourceLocation RENDERING = ResourceLocation.fromNamespaceAndPath("minecraft", "rendering");
        public static final ResourceLocation WINDOW = ResourceLocation.fromNamespaceAndPath("minecraft", "window");
        public static final ResourceLocation INDICATORS = ResourceLocation.fromNamespaceAndPath("minecraft", "indicators");
        public static final ResourceLocation GRAPHICS = ResourceLocation.fromNamespaceAndPath("minecraft", "graphics");
        public static final ResourceLocation MIPMAPS = ResourceLocation.fromNamespaceAndPath("minecraft", "mipmaps");
        public static final ResourceLocation DETAILS = ResourceLocation.fromNamespaceAndPath("minecraft", "details");
        public static final ResourceLocation CHUNK_UPDATES = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "chunk_updates");
        public static final ResourceLocation RENDERING_CULLING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "rendering_culling");
        public static final ResourceLocation CPU_SAVING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "cpu_saving");
        public static final ResourceLocation SORTING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "sorting");
    }

    public static class Pages {
        public static final OptionIdentifier<Void> GENERAL = OptionIdentifier.create(SodiumClientMod.MODID, "general");
        public static final OptionIdentifier<Void> QUALITY = OptionIdentifier.create(SodiumClientMod.MODID, "quality");
        public static final OptionIdentifier<Void> PERFORMANCE = OptionIdentifier.create(SodiumClientMod.MODID, "performance");
        public static final OptionIdentifier<Void> ADVANCED = OptionIdentifier.create(SodiumClientMod.MODID, "advanced");
    }

    public static class Option {
        public static final ResourceLocation RENDER_DISTANCE = ResourceLocation.fromNamespaceAndPath("minecraft", "render_distance");
        public static final ResourceLocation SIMULATION_DISTANCE = ResourceLocation.fromNamespaceAndPath("minecraft", "simulation_distance");
        public static final ResourceLocation BRIGHTNESS = ResourceLocation.fromNamespaceAndPath("minecraft", "brightness");
        public static final ResourceLocation GUI_SCALE = ResourceLocation.fromNamespaceAndPath("minecraft", "gui_scale");
        public static final ResourceLocation FULLSCREEN = ResourceLocation.fromNamespaceAndPath("minecraft", "fullscreen");
        public static final ResourceLocation VSYNC = ResourceLocation.fromNamespaceAndPath("minecraft", "vsync");
        public static final ResourceLocation MAX_FRAMERATE = ResourceLocation.fromNamespaceAndPath("minecraft", "max_frame_rate");
        public static final ResourceLocation VIEW_BOBBING = ResourceLocation.fromNamespaceAndPath("minecraft", "view_bobbing");
        public static final ResourceLocation ATTACK_INDICATOR = ResourceLocation.fromNamespaceAndPath("minecraft", "attack_indicator");
        public static final ResourceLocation AUTOSAVE_INDICATOR = ResourceLocation.fromNamespaceAndPath("minecraft", "autosave_indicator");
        public static final ResourceLocation GRAPHICS_MODE = ResourceLocation.fromNamespaceAndPath("minecraft", "graphics_mode");
        public static final ResourceLocation CLOUDS = ResourceLocation.fromNamespaceAndPath("minecraft", "clouds");
        public static final ResourceLocation WEATHER = ResourceLocation.fromNamespaceAndPath("minecraft", "weather");
        public static final ResourceLocation LEAVES = ResourceLocation.fromNamespaceAndPath("minecraft", "leaves");
        public static final ResourceLocation PARTICLES = ResourceLocation.fromNamespaceAndPath("minecraft", "particles");
        public static final ResourceLocation SMOOTH_LIGHT = ResourceLocation.fromNamespaceAndPath("minecraft", "smooth_lighting");
        public static final ResourceLocation BIOME_BLEND = ResourceLocation.fromNamespaceAndPath("minecraft", "biome_blend");
        public static final ResourceLocation ENTITY_DISTANCE = ResourceLocation.fromNamespaceAndPath("minecraft", "entity_distance");
        public static final ResourceLocation ENTITY_SHADOWS = ResourceLocation.fromNamespaceAndPath("minecraft", "entity_shadows");
        public static final ResourceLocation VIGNETTE = ResourceLocation.fromNamespaceAndPath("minecraft", "vignette");
        public static final ResourceLocation MIPMAP_LEVEL = ResourceLocation.fromNamespaceAndPath("minecraft", "mipmap_levels");
        public static final ResourceLocation CHUNK_UPDATE_THREADS = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "chunk_update_threads");
        public static final ResourceLocation DEFFER_CHUNK_UPDATES = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "defer_chunk_updates");
        public static final ResourceLocation BLOCK_FACE_CULLING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "block_face_culling");
        public static final ResourceLocation COMPACT_VERTEX_FORMAT = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "compact_vertex_format");
        public static final ResourceLocation FOG_OCCLUSION = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "fog_occlusion");
        public static final ResourceLocation ENTITY_CULLING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "entity_culling");
        public static final ResourceLocation ANIMATE_VISIBLE_TEXTURES = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "animate_only_visible_textures");
        public static final ResourceLocation NO_ERROR_CONTEXT = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "no_error_context");
        public static final ResourceLocation PERSISTENT_MAPPING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "persistent_mapping");
        public static final ResourceLocation CPU_FRAMES_AHEAD = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "cpu_render_ahead_limit");
        public static final ResourceLocation TRANSLUCENT_FACE_SORTING = ResourceLocation.fromNamespaceAndPath(SodiumClientMod.MODID, "translucent_face_sorting");
    }
}
