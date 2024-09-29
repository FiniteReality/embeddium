package org.embeddedt.embeddium.api.options.structure;

import org.embeddedt.embeddium.impl.Embeddium;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.options.OptionIdentifier;

public final class StandardOptions {
    public static class Group {
        public static final ResourceLocation RENDERING = ResourceLocation.fromNamespaceAndPath("minecraft", "rendering");
        public static final ResourceLocation WINDOW = ResourceLocation.fromNamespaceAndPath("minecraft", "window");
        public static final ResourceLocation INDICATORS = ResourceLocation.fromNamespaceAndPath("minecraft", "indicators");
        public static final ResourceLocation GRAPHICS = ResourceLocation.fromNamespaceAndPath("minecraft", "graphics");
        public static final ResourceLocation MIPMAPS = ResourceLocation.fromNamespaceAndPath("minecraft", "mipmaps");
        public static final ResourceLocation DETAILS = ResourceLocation.fromNamespaceAndPath("minecraft", "details");
        public static final ResourceLocation CHUNK_UPDATES = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "chunk_updates");
        public static final ResourceLocation RENDERING_CULLING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "rendering_culling");
        public static final ResourceLocation CPU_SAVING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "cpu_saving");
        public static final ResourceLocation SORTING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "sorting");
        public static final ResourceLocation LIGHTING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "lighting");
    }

    public static class Pages {
        public static final OptionIdentifier<Void> GENERAL = OptionIdentifier.create(Embeddium.MODID, "general");
        public static final OptionIdentifier<Void> QUALITY = OptionIdentifier.create(Embeddium.MODID, "quality");
        public static final OptionIdentifier<Void> PERFORMANCE = OptionIdentifier.create(Embeddium.MODID, "performance");
        public static final OptionIdentifier<Void> ADVANCED = OptionIdentifier.create(Embeddium.MODID, "advanced");
    }

    public static class Option {
        public static final ResourceLocation RENDER_DISTANCE = ResourceLocation.fromNamespaceAndPath("minecraft", "render_distance");
        public static final ResourceLocation SIMULATION_DISTANCE = ResourceLocation.fromNamespaceAndPath("minecraft", "simulation_distance");
        public static final ResourceLocation BRIGHTNESS = ResourceLocation.fromNamespaceAndPath("minecraft", "brightness");
        public static final ResourceLocation GUI_SCALE = ResourceLocation.fromNamespaceAndPath("minecraft", "gui_scale");
        public static final ResourceLocation FULLSCREEN = ResourceLocation.fromNamespaceAndPath("minecraft", "fullscreen");
        public static final ResourceLocation FULLSCREEN_RESOLUTION = ResourceLocation.fromNamespaceAndPath("minecraft", "fullscreen_resolution");
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
        public static final ResourceLocation CHUNK_UPDATE_THREADS = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "chunk_update_threads");
        public static final ResourceLocation DEFFER_CHUNK_UPDATES = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "defer_chunk_updates");
        public static final ResourceLocation BLOCK_FACE_CULLING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "block_face_culling");
        public static final ResourceLocation COMPACT_VERTEX_FORMAT = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "compact_vertex_format");
        public static final ResourceLocation FOG_OCCLUSION = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "fog_occlusion");
        public static final ResourceLocation ENTITY_CULLING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "entity_culling");
        public static final ResourceLocation ANIMATE_VISIBLE_TEXTURES = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "animate_only_visible_textures");
        public static final ResourceLocation NO_ERROR_CONTEXT = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "no_error_context");
        public static final ResourceLocation PERSISTENT_MAPPING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "persistent_mapping");
        public static final ResourceLocation CPU_FRAMES_AHEAD = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "cpu_render_ahead_limit");
        public static final ResourceLocation TRANSLUCENT_FACE_SORTING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "translucent_face_sorting");
        public static final ResourceLocation USE_QUAD_NORMALS_FOR_LIGHTING = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "use_quad_normals_for_lighting");
        public static final ResourceLocation RENDER_PASS_OPTIMIZATION = ResourceLocation.fromNamespaceAndPath(Embeddium.MODID, "render_pass_optimization");
    }
}
