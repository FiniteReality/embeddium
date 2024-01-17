package me.jellysquid.mods.sodium.mixin.features.entity.smooth_lighting;

import com.mojang.blaze3d.vertex.PoseStack;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.model.light.EntityLighter;
import me.jellysquid.mods.sodium.client.render.entity.EntityLightSampler;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.PaintingRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.decoration.Painting;
import net.minecraft.world.level.BlockAndTintGetter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PaintingRenderer.class)
public abstract class MixinPaintingEntityRenderer extends EntityRenderer<Painting> implements EntityLightSampler<Painting> {
    private Painting entity;
    private float tickDelta;

    protected MixinPaintingEntityRenderer(EntityRenderDispatcher dispatcher) {
        super(dispatcher);
    }

    @Inject(method = "render", at = @At(value = "HEAD"))
    public void preRender(Painting paintingEntity, float f, float g, PoseStack matrixStack, MultiBufferSource vertexConsumerProvider, int i, CallbackInfo ci) {
        this.entity = paintingEntity;
        this.tickDelta = g;
    }

    /**
     * @author FlashyReese
     * @reason Redirect Lightmap coord with Sodium's EntityLighter.
     */
    @Redirect(method = "func_229122_a_", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/LevelRenderer;getLightColor(Lnet/minecraft/world/level/BlockAndTintGetter;Lnet/minecraft/core/BlockPos;)I"))
    public int redirectLightmapCoord(BlockAndTintGetter world, BlockPos pos) {
        if (SodiumClientMod.options().quality.smoothLighting == SodiumGameOptions.LightingQuality.HIGH && this.entity != null) {
            return EntityLighter.getBlendedLight(this, this.entity, tickDelta);
        } else {
            return LevelRenderer.getLightColor(world, pos);
        }
    }

    @Override
    public int bridge$getBlockLight(Painting entity, BlockPos pos) {
        return this.getBlockLightLevel(entity, pos);
    }

    @Override
    public int bridge$getSkyLight(Painting entity, BlockPos pos) {
        return this.getSkyLightLevel(entity, pos);
    }
}