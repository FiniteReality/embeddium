package org.embeddedt.embeddium.impl.render.frapi;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.util.RandomSource;
import org.embeddedt.embeddium.api.render.chunk.BlockRenderContext;
import org.embeddedt.embeddium.impl.render.chunk.compile.ChunkBuildBuffers;

public interface FRAPIRenderHandler {
    boolean INDIGO_PRESENT = isIndigoPresent();

    private static boolean isIndigoPresent() {
        boolean indigoPresent = false;
        try {
            Class.forName("net.fabricmc.fabric.impl.client.indigo.renderer.render.BlockRenderContext");
            indigoPresent = true;
        } catch(Throwable ignored) {}
        return indigoPresent;
    }

    void reset();

    void renderEmbeddium(BlockRenderContext ctx, ChunkBuildBuffers buffers, PoseStack mStack, RandomSource random);
}
