package org.embeddedt.embeddium.impl.mixin.features.shader.uniform;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.mojang.blaze3d.shaders.Uniform;
import com.mojang.blaze3d.systems.RenderSystem;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.ShaderInstance;

/**
 * On the NVIDIA drivers (and maybe some others), the OpenGL submission thread requires expensive state synchronization
 * to happen when glGetUniformLocation and glGetInteger are called. In our case, this is rather unnecessary, since
 * these uniform locations can be trivially cached.
 */
// High priority so replacement happens before other mods increase the sampler count, so that we see the updated value
@Mixin(value = ShaderInstance.class, priority = 500)
public abstract class ShaderProgramMixin {
    @Shadow
    @Final
    private List<String> samplerNames;

    @Shadow
    @Final
    private int programId;

    @Shadow
    @Final
    private List<Uniform> uniforms;

    @Shadow
    public abstract void setSampler(String p_173351_, Object p_173352_);

    @Unique
    private Object2IntMap<String> uniformCache;

    @Redirect(method = "apply", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/shaders/Uniform;glGetUniformLocation(ILjava/lang/CharSequence;)I"))
    private int redirectGetUniformLocation(int program, CharSequence name) {
        if(this.uniformCache == null) {
            this.uniformCache = new Object2IntOpenHashMap<>();
            this.uniformCache.defaultReturnValue(-1);

            for (var samplerName : this.samplerNames) {
                var location = Uniform.glGetUniformLocation(this.programId, samplerName);

                if(location != -1)
                    this.uniformCache.put(samplerName, location);
            }
        }
        var location = this.uniformCache.getInt(name);

        if (location == -1) {
            throw new IllegalStateException("Failed to find uniform '%s' during shader bind".formatted(name));
        }

        return location;
    }

    @Redirect(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/ShaderInstance;uniforms:Ljava/util/List;", ordinal = 0))
    private List<Uniform> uploadUniforms(ShaderInstance instance) {
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
            setSampler(samplerIds[i], RenderSystem.getShaderTexture(i));
        }
        return 0;
    }
}
