package me.jellysquid.mods.sodium.mixin.features.textures.animations.tracking;

import com.mojang.blaze3d.vertex.VertexConsumer;
import me.jellysquid.mods.sodium.client.render.texture.SpriteUtil;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(TextureSheetParticle.class)
public abstract class SpriteBillboardParticleMixin extends SingleQuadParticle {
    @Shadow
    protected TextureAtlasSprite sprite;

    @Unique
    private boolean shouldTickSprite;

    protected SpriteBillboardParticleMixin(ClientLevel world, double x, double y, double z) {
        super(world, x, y, z);
    }

    @Inject(method = "setSprite(Lnet/minecraft/client/renderer/texture/TextureAtlasSprite;)V", at = @At("RETURN"))
    private void afterSetSprite(TextureAtlasSprite sprite, CallbackInfo ci) {
        this.shouldTickSprite = sprite != null && SpriteUtil.hasAnimation(sprite);
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        if (this.shouldTickSprite) {
            SpriteUtil.markSpriteActive(this.sprite);
        }

        super.render(vertexConsumer, camera, tickDelta);
    }
}