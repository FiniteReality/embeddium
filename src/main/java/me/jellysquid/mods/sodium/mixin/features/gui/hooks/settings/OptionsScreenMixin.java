package me.jellysquid.mods.sodium.mixin.features.gui.hooks.settings;

import net.minecraft.client.gui.screens.OptionsScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.embeddedt.embeddium.gui.EmbeddiumVideoOptionsScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(OptionsScreen.class)
public class OptionsScreenMixin extends Screen {
    protected OptionsScreenMixin(Component title) {
        super(title);
    }

    @Inject(method = "lambda$init$5", at = @At("HEAD"), cancellable = true)
    private void open(CallbackInfo ci) {
        this.minecraft.setScreen(new EmbeddiumVideoOptionsScreen(this));
        ci.cancel();
    }
}
