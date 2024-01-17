package me.jellysquid.mods.sodium.client.render.chunk.shader;

import me.jellysquid.mods.sodium.client.gl.shader.GlProgram;
import me.jellysquid.mods.sodium.client.gl.device.RenderDevice;
import me.jellysquid.mods.sodium.client.render.GameRendererContext;
import net.minecraft.resources.ResourceLocation;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.system.MemoryStack;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.function.Function;

/**
 * A forward-rendering shader program for chunks.
 */
public class ChunkProgram extends GlProgram {
    // Uniform variable binding indexes
    private final int uModelViewProjectionMatrix;
    private final int uModelScale;
    private final int uTextureScale;
    private final int uBlockTex;
    private final int uLightTex;

    // The fog shader component used by this program in order to setup the appropriate GL state
    private final ChunkShaderFogComponent fogShader;

    protected ChunkProgram(RenderDevice owner, ResourceLocation name, int handle, Function<ChunkProgram, ChunkShaderFogComponent> fogShaderFunction) {
        super(owner, name, handle);

        this.uModelViewProjectionMatrix = this.getUniformLocation("u_ModelViewProjectionMatrix");

        this.uBlockTex = this.getUniformLocation("u_BlockTex");
        this.uLightTex = this.getUniformLocation("u_LightTex");
        this.uModelScale = this.getUniformLocation("u_ModelScale");
        this.uTextureScale = this.getUniformLocation("u_TextureScale");

        this.fogShader = fogShaderFunction.apply(this);
    }

    public void setup(PoseStack matrixStack, float modelScale, float textureScale) {
        GlStateManager._glUniform1i(this.uBlockTex, 0);
        GlStateManager._glUniform1i(this.uLightTex, 2);

        GL20C.glUniform3f(this.uModelScale, modelScale, modelScale, modelScale);
        GL20C.glUniform2f(this.uTextureScale, textureScale, textureScale);

        this.fogShader.setup();

        try (MemoryStack memoryStack = MemoryStack.stackPush()) {
        	GlStateManager._glUniformMatrix4(this.uModelViewProjectionMatrix, false,
                    GameRendererContext.getModelViewProjectionMatrix(matrixStack.last(), memoryStack));
        }
    }
}
