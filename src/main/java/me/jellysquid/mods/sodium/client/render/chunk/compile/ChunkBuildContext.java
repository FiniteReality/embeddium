package me.jellysquid.mods.sodium.client.render.chunk.compile;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.jellysquid.mods.sodium.client.render.chunk.vertex.format.ChunkVertexType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import me.jellysquid.mods.sodium.client.render.chunk.compile.pipeline.BlockRenderCache;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;

import java.util.Collections;

public class ChunkBuildContext {
    public final ChunkBuildBuffers buffers;
    public final BlockRenderCache cache;
    private final ObjectOpenHashSet<TextureAtlasSprite> additionalCapturedSprites;
    private boolean captureAdditionalSprites;

    public ChunkBuildContext(ClientLevel world, ChunkVertexType vertexType) {
        this.buffers = new ChunkBuildBuffers(vertexType);
        this.cache = new BlockRenderCache(Minecraft.getInstance(), world);
        this.additionalCapturedSprites = new ObjectOpenHashSet<>();
    }

    public void cleanup() {
        this.buffers.destroy();
        this.cache.cleanup();
        this.additionalCapturedSprites.clear();
        this.captureAdditionalSprites = false;
    }

    public void setCaptureAdditionalSprites(boolean flag) {
        captureAdditionalSprites = flag;
        if(!flag) {
            additionalCapturedSprites.clear();
        }
    }

    public Iterable<TextureAtlasSprite> getAdditionalCapturedSprites() {
        return additionalCapturedSprites.isEmpty() ? Collections.emptySet() : additionalCapturedSprites;
    }

    public void captureAdditionalSprite(TextureAtlasSprite sprite) {
        if(captureAdditionalSprites) {
            additionalCapturedSprites.add(sprite);
        }
    }
}
