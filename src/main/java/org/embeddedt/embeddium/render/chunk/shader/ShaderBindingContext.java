package org.embeddedt.embeddium.render.chunk.shader;

import org.embeddedt.embeddium.gl.shader.uniform.GlUniform;
import org.embeddedt.embeddium.gl.shader.uniform.GlUniformBlock;

import java.util.function.IntFunction;

public interface ShaderBindingContext {
    <U extends GlUniform<?>> U bindUniform(String name, IntFunction<U> factory);

    GlUniformBlock bindUniformBlock(String name, int bindingPoint);
}
