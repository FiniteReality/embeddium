package me.jellysquid.mods.sodium.mixin.features.render.world.clouds;

import net.minecraft.client.renderer.FogRenderer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(FogRenderer.class)
public interface BackgroundRendererInvoker {
    @Invoker("getPriorityFogFunction")
    static FogRenderer.MobEffectFogFunction invokeGetFogModifier(Entity entity, float tickDelta) {
        throw new AssertionError();
    }
}
