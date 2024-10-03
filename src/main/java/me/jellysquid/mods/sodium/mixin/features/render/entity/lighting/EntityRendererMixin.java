package me.jellysquid.mods.sodium.mixin.features.render.entity.lighting;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.model.light.EntityLighter;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<T extends Entity> implements EntityLighter.LightSampler<T> {
    @Shadow
    protected abstract int getBlockLightLevel(T entity, BlockPos pos);

    @Shadow
    protected abstract int getSkyLightLevel(T entity, BlockPos pos);

    @Inject(method = "getPackedLightCoords", at = @At("HEAD"), cancellable = true)
    private void preGetLight(T entity, float tickDelta, CallbackInfoReturnable<Integer> cir) {
        // Use smooth entity lighting if enabled
        if (SodiumClientMod.options().quality.useSmoothEntityLighting) {
            cir.setReturnValue(EntityLighter.getBlendedLight(this, entity, tickDelta));
        }
    }

    @Override
    public int bridge$getBlockLight(T entity, BlockPos pos) {
        return this.getBlockLightLevel(entity, pos);
    }

    @Override
    public int bridge$getSkyLight(T entity, BlockPos pos) {
        return this.getSkyLightLevel(entity, pos);
    }
}
