package me.jellysquid.mods.sodium.mixin.prelaunch;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import me.jellysquid.mods.sodium.SodiumPreLaunch;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;

@Mixin(MinecraftClient.class)
public class MixinPreLaunch {

    @Inject(method = "<init>", at = @At("HEAD"))
    public void rubidium$preLaunch(RunArgs arg, CallbackInfo ci) {
        SodiumPreLaunch.onPreLaunch();
    }
	
}
