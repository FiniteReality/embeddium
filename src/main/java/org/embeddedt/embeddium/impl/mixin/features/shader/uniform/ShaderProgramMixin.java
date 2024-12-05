package org.embeddedt.embeddium.impl.mixin.features.shader.uniform;

import com.mojang.blaze3d.shaders.Uniform;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.Collections;
import java.util.List;

import net.minecraft.client.renderer.CompiledShaderProgram;

/**
 * On the NVIDIA drivers (and maybe some others), the OpenGL submission thread requires expensive state synchronization
 * to happen when glGetUniformLocation and glGetInteger are called. In our case, this is rather unnecessary, since
 * these uniform locations can be trivially cached.
 */
// High priority so replacement happens before other mods increase the sampler count, so that we see the updated value
@Mixin(value = CompiledShaderProgram.class, priority = 500)
public abstract class ShaderProgramMixin {
    @Shadow
    @Final
    private List<Uniform> uniforms;

    @Redirect(method = "apply", at = @At(value = "FIELD", target = "Lnet/minecraft/client/renderer/CompiledShaderProgram;uniforms:Ljava/util/List;", ordinal = 0))
    private List<Uniform> uploadUniforms(CompiledShaderProgram instance) {
        List<Uniform> uniforms = this.uniforms;
        //noinspection ForLoopReplaceableByForEach
        for(int i = 0; i < uniforms.size(); i++) {
            uniforms.get(i).upload();
        }
        return Collections.emptyList();
    }
}
