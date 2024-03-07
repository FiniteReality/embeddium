package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.stream.IntStream;

@Mixin(VertexBuffer.class)
public class VertexBufferMixin {
    private static final int NUM_SAMPLERS = 12;
    private static final String[] SAMPLER_IDS = IntStream.range(0, NUM_SAMPLERS).mapToObj(i -> "Sampler" + i).toArray(String[]::new);

    /**
     * @author embeddedt
     * @reason Avoid regenerating the sampler ID strings every time a buffer is drawn
     */
    @ModifyConstant(method = "_drawWithShader", constant = @Constant(intValue = NUM_SAMPLERS, ordinal = 0))
    private int setSamplersManually(int constant, Matrix4f mat1, Matrix4f mat2, ShaderInstance shader) {
        for(int i = 0; i < constant; i++) {
            shader.setSampler(SAMPLER_IDS[i], RenderSystem.getShaderTexture(i));
        }
        return 0;
    }
}
