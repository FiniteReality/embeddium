package org.embeddedt.embeddium.impl.render.chunk.shader;

import com.mojang.blaze3d.platform.GlStateManager;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformFloat3v;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformInt;
import org.embeddedt.embeddium.impl.gl.shader.uniform.GlUniformMatrix4f;
import org.embeddedt.embeddium.impl.util.TextureUtil;
import org.joml.Matrix4fc;
import org.lwjgl.opengl.GL32C;

import java.util.EnumMap;
import java.util.Map;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkShaderInterface {
    private final Map<ChunkShaderTextureSlot, GlUniformInt> uniformTextures;

    private final GlUniformMatrix4f uniformModelViewMatrix;
    private final GlUniformMatrix4f uniformProjectionMatrix;
    private final GlUniformFloat3v uniformRegionOffset;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    public ChunkShaderInterface(ShaderBindingContext context, ChunkShaderOptions options) {
        this.uniformModelViewMatrix = context.bindUniform("u_ModelViewMatrix", GlUniformMatrix4f::new);
        this.uniformProjectionMatrix = context.bindUniform("u_ProjectionMatrix", GlUniformMatrix4f::new);
        this.uniformRegionOffset = context.bindUniform("u_RegionOffset", GlUniformFloat3v::new);

        this.uniformTextures = new EnumMap<>(ChunkShaderTextureSlot.class);
        this.uniformTextures.put(ChunkShaderTextureSlot.BLOCK, context.bindUniform("u_BlockTex", GlUniformInt::new));
        this.uniformTextures.put(ChunkShaderTextureSlot.LIGHT, context.bindUniform("u_LightTex", GlUniformInt::new));

        this.fogShader = options.fog().getFactory().apply(context);
    }

    public void setupState() {
        this.bindTexture(ChunkShaderTextureSlot.BLOCK, TextureUtil.getBlockTextureId());
        this.bindTexture(ChunkShaderTextureSlot.LIGHT, TextureUtil.getLightTextureId());

        this.fogShader.setup();
    }

    private void bindTexture(ChunkShaderTextureSlot slot, int textureId) {
        GlStateManager._activeTexture(GL32C.GL_TEXTURE0 + slot.ordinal());
        GlStateManager._bindTexture(textureId);

        var uniform = this.uniformTextures.get(slot);
        uniform.setInt(slot.ordinal());
    }

    public void setProjectionMatrix(Matrix4fc matrix) {
        this.uniformProjectionMatrix.set(matrix);
    }

    public void setModelViewMatrix(Matrix4fc matrix) {
        this.uniformModelViewMatrix.set(matrix);
    }

    public void setRegionOffset(float x, float y, float z) {
        this.uniformRegionOffset.set(x, y, z);
    }
}
