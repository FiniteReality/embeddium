package me.jellysquid.mods.sodium.mixin.features.chunk_rendering;

import com.google.common.collect.ImmutableList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;
import net.minecraft.client.renderer.RenderType;

@Mixin(RenderType.class)
public class MixinRenderLayer {
    @Unique
    private static final List<RenderType> embeddium$blockLayers = ImmutableList.of(RenderType.solid(), RenderType.cutoutMipped(), RenderType.cutout(), RenderType.translucent(), RenderType.tripwire());

    /**
     * @author Kasualix
     * @reason Don't create an immutableList every time this is called.
     */
    @Overwrite
    public static List<RenderType> chunkBufferLayers() {
        return embeddium$blockLayers;
    }
}
