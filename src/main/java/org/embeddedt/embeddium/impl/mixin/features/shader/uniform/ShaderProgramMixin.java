package org.embeddedt.embeddium.impl.mixin.features.shader.uniform;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.renderer.CompiledShaderProgram;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

// High priority so replacement happens before other mods increase the sampler count, so that we see the updated value
@Mixin(value = CompiledShaderProgram.class, priority = 500)
public abstract class ShaderProgramMixin {
    @Shadow
    @Final
    private List<Uniform> uniforms;

    @Shadow
    public abstract void bindSampler(String samplerName, int obj);

    /**
     * @author embeddedt
     * @reason avoid iterator allocation in frequently called code
     */
    @Redirect(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;uniforms:Ljava/util/List;", ordinal = 0))
    private List<Uniform> uploadUniforms(CompiledShaderProgram instance) {
        List<Uniform> uniforms = this.uniforms;
        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < uniforms.size(); i++) {
            uniforms.get(i).upload();
        }
        return Collections.emptyList();
    }

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
    @ModifyExpressionValue(method = "setDefaultUniforms", at = @At(value = "CONSTANT", args = "intValue=" + DEFAULT_NUM_SAMPLERS, ordinal = 0))
    private int setSamplersManually(int numSamplers) {
        String[] samplerIds = SAMPLER_IDS;
        if (samplerIds.length < numSamplers) {
            samplerIds = embeddium$makeSamplerIds(numSamplers);
            SAMPLER_IDS = samplerIds;
        }
        for(int i = 0; i < numSamplers; i++) {
            bindSampler(samplerIds[i], RenderSystem.getShaderTexture(i));
        }
        return 0;
    }
}
