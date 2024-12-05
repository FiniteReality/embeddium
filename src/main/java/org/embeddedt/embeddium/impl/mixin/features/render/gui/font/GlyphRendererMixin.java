package org.embeddedt.embeddium.impl.mixin.features.render.gui.font;

import com.mojang.blaze3d.vertex.VertexConsumer;
import org.embeddedt.embeddium.api.util.ColorARGB;
import org.embeddedt.embeddium.api.vertex.format.common.GlyphVertex;
import net.minecraft.client.gui.font.glyphs.BakedGlyph;
import org.embeddedt.embeddium.api.vertex.buffer.VertexBufferWriter;
import org.embeddedt.embeddium.api.util.ColorABGR;
import org.embeddedt.embeddium.api.math.MatrixHelper;
import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BakedGlyph.class)
public class GlyphRendererMixin {
    @Shadow
    @Final
    private float left;

    @Shadow
    @Final
    private float right;

    @Shadow
    @Final
    private float up;

    @Shadow
    @Final
    private float down;

    @Shadow
    @Final
    private float u0;

    @Shadow
    @Final
    private float v0;

    @Shadow
    @Final
    private float v1;

    @Shadow
    @Final
    private float u1;

    /**
     * @reason Use intrinsics
     * @author JellySquid
     */
    @Inject(method = "render(ZFFFLorg/joml/Matrix4f;Lcom/mojang/blaze3d/vertex/VertexConsumer;IZI)V", at = @At("HEAD"), cancellable = true)
    private void renderFast(boolean italic, float x, float y, float z, Matrix4f matrix, VertexConsumer vertexConsumer, int color, boolean applyBoldScale, int light, CallbackInfo ci) {
        int packedColor = ColorARGB.toABGR(color);
        if (drawFast(italic, x, y, z, matrix, vertexConsumer, packedColor, light, applyBoldScale)) {
            ci.cancel();
        }
    }

    private boolean drawFast(boolean italic, float x, float y, float z, Matrix4f matrix, VertexConsumer vertexConsumer, int color, int light, boolean applyBoldScale) {
        var writer = VertexBufferWriter.tryOf(vertexConsumer);

        if (writer == null)
            return false;

        float x1 = x + this.left;
        float x2 = x + this.right;
        float h1 = y + this.up;
        float h2 = y + this.down;
        float w1 = italic ? 1.0F - 0.25F * this.up : 0.0F;
        float w2 = italic ? 1.0F - 0.25F * this.down : 0.0F;
        float boldScale = applyBoldScale ? 0.1F : 0.0F;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            long buffer = stack.nmalloc(4 * GlyphVertex.STRIDE);
            long ptr = buffer;

            write(ptr, matrix, x1 + w1 - boldScale, h1 - boldScale, z, color, this.u0, this.v0, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x1 + w2 - boldScale, h2 + boldScale, z, color, this.u0, this.v1, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w2 + boldScale, h2 + boldScale, z, color, this.u1, this.v1, light);
            ptr += GlyphVertex.STRIDE;

            write(ptr, matrix, x2 + w1 + boldScale, h1 - boldScale, z, color, this.u1, this.v0, light);
            ptr += GlyphVertex.STRIDE;

            writer.push(stack, buffer, 4, GlyphVertex.FORMAT);
        }

        return true;
    }

    @Unique
    private static void write(long buffer,
                              Matrix4f matrix, float x, float y, float z, int color, float u, float v, int light) {
        float x2 = MatrixHelper.transformPositionX(matrix, x, y, z);
        float y2 = MatrixHelper.transformPositionY(matrix, x, y, z);
        float z2 = MatrixHelper.transformPositionZ(matrix, x, y, z);

        GlyphVertex.put(buffer, x2, y2, z2, color, u, v, light);
    }

}
