package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

// High priority so replacement happens before other mods increase the sampler count, so that we see the updated value
@Mixin(value = VertexBuffer.class, priority = 500)
public class VertexBufferMixin {
    private static final int DEFAULT_NUM_SAMPLERS = 12;
    private static String[] SAMPLER_IDS = embeddium$makeSamplerIds(DEFAULT_NUM_SAMPLERS);

    private static String[] embeddium$makeSamplerIds(int len) {
        String[] samplerIds = new String[len];
        for(int i = 0; i < len; i++) {
            samplerIds[i] = "Sampler" + i;
        }
        return samplerIds;
    }

    /**
     * @author embeddedt
     * @reason Avoid regenerating the sampler ID strings every time a buffer is drawn
     */
    @ModifyExpressionValue(method = "_drawWithShader", at = @At(value = "CONSTANT", args = "intValue=" + DEFAULT_NUM_SAMPLERS, ordinal = 0))
    private int setSamplersManually(int numSamplers, Matrix4f mat1, Matrix4f mat2, ShaderInstance shader) {
        String[] samplerIds = SAMPLER_IDS;
        if (samplerIds.length < numSamplers) {
            samplerIds = embeddium$makeSamplerIds(numSamplers);
            SAMPLER_IDS = samplerIds;
        }
        for(int i = 0; i < numSamplers; i++) {
            shader.setSampler(samplerIds[i], RenderSystem.getShaderTexture(i));
        }
        return 0;
    }
}
