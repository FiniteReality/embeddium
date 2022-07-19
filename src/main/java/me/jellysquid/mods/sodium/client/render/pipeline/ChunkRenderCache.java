package me.jellysquid.mods.sodium.client.render.pipeline;

import me.jellysquid.mods.sodium.client.model.quad.blender.ColorBlender;
import net.minecraft.client.MinecraftClient;

public class ChunkRenderCache {
    protected ColorBlender createBiomeColorBlender() {
    	return ColorBlender.create(MinecraftClient.getInstance());
    }
}
