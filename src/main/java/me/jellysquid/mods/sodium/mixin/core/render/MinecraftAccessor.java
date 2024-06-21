package me.jellysquid.mods.sodium.mixin.core.render;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
    @Accessor("gameThread")
    Thread embeddium$getGameThread();
}
