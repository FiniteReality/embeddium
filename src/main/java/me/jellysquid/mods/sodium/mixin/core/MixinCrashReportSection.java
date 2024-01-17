package me.jellysquid.mods.sodium.mixin.core;

import net.minecraft.CrashReportCategory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReportCategory.class)
public class MixinCrashReportSection {
    @Shadow private StackTraceElement[] stackTrace;

    /**
     * Hacky fix so that our crash reports will not just have NegativeArraySizeException.
     */
    @Inject(method = "trimStacktrace", at = @At("HEAD"), cancellable = true)
    private void preventArraySizeIssue(int callCount, CallbackInfo ci) {
        if((this.stackTrace.length - callCount) < 0) {
            System.out.println("Suppressing NegativeArraySizeException in buggy Mojang crash handler");
            ci.cancel();
        }
    }
}
