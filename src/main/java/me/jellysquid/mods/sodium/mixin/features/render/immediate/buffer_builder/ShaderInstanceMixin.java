package me.jellysquid.mods.sodium.mixin.features.render.immediate.buffer_builder;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.VertexBuffer;
import net.minecraft.client.renderer.ShaderInstance;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

import java.util.stream.IntStream;

@Mixin(ShaderInstance.class)
public abstract class ShaderInstanceMixin {
    @Shadow
    public abstract void setSampler(String string, Object object);

    private static final int NUM_SAMPLERS = 12;
    private static final String[] SAMPLER_IDS = IntStream.range(0, NUM_SAMPLERS).mapToObj(i -> "Sampler" + i).toArray(String[]::new);

    /**
     * @author embeddedt
     * @reason Avoid regenerating the sampler ID strings every time a buffer is drawn
     */
    @ModifyConstant(method = "setDefaultUniforms", constant = @Constant(intValue = NUM_SAMPLERS, ordinal = 0))
    private int setSamplersManually(int constant) {
        for(int i = 0; i < constant; i++) {
            this.setSampler(SAMPLER_IDS[i], RenderSystem.getShaderTexture(i));
        }
        return 0;
    }
}
