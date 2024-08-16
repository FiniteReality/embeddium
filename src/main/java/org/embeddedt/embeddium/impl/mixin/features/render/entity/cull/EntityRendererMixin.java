package org.embeddedt.embeddium.impl.mixin.features.render.entity.cull;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.world.entity.Leashable;
import org.embeddedt.embeddium.impl.render.EmbeddiumWorldRenderer;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(EntityRenderer.class)
@SuppressWarnings({"rawtypes", "unchecked"})
public abstract class EntityRendererMixin<T extends Entity> {
    @ModifyExpressionValue(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 0))
    private boolean checkSectionForCullingMain(boolean isWithinFrustum, @Local(ordinal = 0, argsOnly = true) T entity) {
        if(!isWithinFrustum) {
            return false;
        }

        // Check if the entity is in a visible chunk section

        var renderer = EmbeddiumWorldRenderer.instanceNullable();

        return renderer == null || renderer.isEntityVisible(entity, (EntityRenderer)(Object)this);
    }

    @ModifyExpressionValue(method = "shouldRender", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/culling/Frustum;isVisible(Lnet/minecraft/world/phys/AABB;)Z", ordinal = 1))
    private boolean checkSectionForCullingMain(boolean isWithinFrustum, @Local(ordinal = 0) Leashable leashable) {
        if(!isWithinFrustum) {
            return false;
        }

        // Check if the entity is in a visible chunk section

        var renderer = EmbeddiumWorldRenderer.instanceNullable();

        return renderer == null || renderer.isEntityVisible(leashable.getLeashHolder(), (EntityRenderer)(Object)this);
    }
}
