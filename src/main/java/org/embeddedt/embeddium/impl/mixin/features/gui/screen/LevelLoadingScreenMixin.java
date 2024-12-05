package org.embeddedt.embeddium.impl.mixin.features.gui.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Reference2IntOpenHashMap;
import net.minecraft.client.renderer.CoreShaders;
import org.embeddedt.embeddium.api.vertex.format.common.ColorVertex;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.util.ColorARGB;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.LevelLoadingScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.server.level.progress.StoringChunkProgressListener;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;

/**
 * Re-implements the loading screen with considerations to reduce draw calls and other sources of overhead. This can
 * improve world load times on slower processors with very few cores.
 */
@Mixin(LevelLoadingScreen.class)
public class LevelLoadingScreenMixin {
    @Mutable
    @Shadow
    @Final
    private static Object2IntMap<ChunkStatus> COLORS;

    @Unique
    private static Reference2IntOpenHashMap<ChunkStatus> STATUS_TO_COLOR_FAST;

    @Unique
    private static final int NULL_STATUS_COLOR = ColorABGR.pack(0, 0, 0, 0xFF);

    @Unique
    private static final int DEFAULT_STATUS_COLOR = ColorARGB.pack(0, 0x11, 0xFF, 0xFF);

    /**
     * This implementation differs from vanilla's in the following key ways.
     * - All tiles are batched together in one draw call, reducing CPU overhead by an order of magnitudes.
     * - Reference hashing is used for faster ChunkStatus -> Color lookup.
     * - Colors are stored in ABGR format so conversion is not necessary every tile draw.
     *
     * @reason Significantly optimized implementation.
     * @author JellySquid
     */
    @Overwrite
    public static void renderChunks(GuiGraphics drawContext, StoringChunkProgressListener tracker, int mapX, int mapY, int mapScale, int mapPadding) {
        if (STATUS_TO_COLOR_FAST == null) {
            STATUS_TO_COLOR_FAST = new Reference2IntOpenHashMap<>(COLORS.size());
            STATUS_TO_COLOR_FAST.put(null, NULL_STATUS_COLOR);
            COLORS.object2IntEntrySet()
                    .forEach(entry -> STATUS_TO_COLOR_FAST.put(entry.getKey(), ColorARGB.toABGR(entry.getIntValue(), 0xFF)));
        }

        RenderSystem.setShader(CoreShaders.POSITION_COLOR);

        Matrix4f matrix = drawContext.pose().last().pose();

        Tesselator tessellator = Tesselator.getInstance();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        
        BufferBuilder bufferBuilder = tessellator.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);

        var writer = VertexBufferWriter.of(bufferBuilder);

        int centerSize = tracker.getFullDiameter();
        int size = tracker.getDiameter();

        int tileSize = mapScale + mapPadding;

        if (mapPadding != 0) {
            int mapRenderCenterSize = centerSize * tileSize - mapPadding;
            int radius = mapRenderCenterSize / 2 + 1;

            addRect(writer, matrix, mapX - radius, mapY - radius, mapX - radius + 1, mapY + radius, DEFAULT_STATUS_COLOR);
            addRect(writer, matrix, mapX + radius - 1, mapY - radius, mapX + radius, mapY + radius, DEFAULT_STATUS_COLOR);
            addRect(writer, matrix, mapX - radius, mapY - radius, mapX + radius, mapY - radius + 1, DEFAULT_STATUS_COLOR);
            addRect(writer, matrix, mapX - radius, mapY + radius - 1, mapX + radius, mapY + radius, DEFAULT_STATUS_COLOR);
        }

        int mapRenderSize = size * tileSize - mapPadding;
        int mapStartX = mapX - mapRenderSize / 2;
        int mapStartY = mapY - mapRenderSize / 2;

        ChunkStatus prevStatus = null;
        int prevColor = NULL_STATUS_COLOR;

        for (int x = 0; x < size; ++x) {
            int tileX = mapStartX + x * tileSize;

            for (int z = 0; z < size; ++z) {
                int tileY = mapStartY + z * tileSize;

                ChunkStatus status = tracker.getStatus(x, z);
                int color;

                if (prevStatus == status) {
                    color = prevColor;
                } else {
                    color = STATUS_TO_COLOR_FAST.getInt(status);

                    prevStatus = status;
                    prevColor = color;
                }

                addRect(writer, matrix, tileX, tileY, tileX + mapScale, tileY + mapScale, color);
            }
        }

        var data = bufferBuilder.build();

        if (data != null) {
            BufferUploader.drawWithShader(data);
        }

        RenderSystem.disableBlend();
    }

    @Unique
    private static void addRect(VertexBufferWriter writer, Matrix4f matrix, int x1, int y1, int x2, int y2, int color) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * ColorVertex.STRIDE);
            long ptr = buffer;

            ColorVertex.put(ptr, matrix, x1, y2, 0, color);
            ptr += ColorVertex.STRIDE;

            ColorVertex.put(ptr, matrix, x2, y2, 0, color);
            ptr += ColorVertex.STRIDE;

            ColorVertex.put(ptr, matrix, x2, y1, 0, color);
            ptr += ColorVertex.STRIDE;

            ColorVertex.put(ptr, matrix, x1, y1, 0, color);
            ptr += ColorVertex.STRIDE;

            writer.push(stack, buffer, 4, ColorVertex.FORMAT);
        }
    }
}
