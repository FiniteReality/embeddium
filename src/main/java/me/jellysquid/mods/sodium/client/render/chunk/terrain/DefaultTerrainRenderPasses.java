package me.jellysquid.mods.sodium.client.render.chunk.terrain;

import net.minecraft.client.renderer.RenderType;
import org.jetbrains.annotations.ApiStatus;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DefaultTerrainRenderPasses {
    public static final TerrainRenderPass SOLID = new TerrainRenderPass(RenderType.solid(), false, false);
    public static final TerrainRenderPass CUTOUT = new TerrainRenderPass(RenderType.cutoutMipped(), false, true);
    public static final TerrainRenderPass TRANSLUCENT = new TerrainRenderPass(RenderType.translucent(), true, false);

    @Deprecated
    public static final TerrainRenderPass[] ALL = new TerrainRenderPass[] { SOLID, CUTOUT, TRANSLUCENT };

    @ApiStatus.Experimental
    public static final Map<RenderType, List<TerrainRenderPass>> RENDER_PASS_MAPPINGS = Map.of(
            RenderType.solid(), List.of(SOLID, CUTOUT),
            RenderType.translucent(), List.of(TRANSLUCENT)
    );

    static {
        if(!RENDER_PASS_MAPPINGS.values().stream().flatMap(Collection::stream).collect(Collectors.toSet()).containsAll(Arrays.asList(ALL))) {
            throw new IllegalStateException("Render pass mappings are not complete");
        }
    }
}
