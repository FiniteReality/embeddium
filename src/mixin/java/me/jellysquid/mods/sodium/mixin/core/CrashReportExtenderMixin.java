package me.jellysquid.mods.sodium.mixin.core;

import net.minecraft.CrashReport;
import net.minecraftforge.logging.CrashReportExtender;
import org.embeddedt.embeddium_integrity.MixinTaintDetector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CrashReportExtender.class)
public class CrashReportExtenderMixin {
    @Inject(method = "addCrashReportHeader", at = @At("HEAD"), remap = false)
    private static void injectEmbeddiumTaintHeader(StringBuilder builder, CrashReport crashReport, CallbackInfo ci) {
        try {
            var mods = MixinTaintDetector.getTaintingMods();
            if(!mods.isEmpty()) {
                builder.append("// Embeddium instance tainted by mods: [").append(String.join(", ", mods)).append("]\n");
                builder.append("// Please do not reach out for Embeddium support without removing these mods first.\n");
                builder.append("// -------\n");
            }
        } catch(Throwable ignored) {
            // fail-safe, we absolutely do not want to crash during crash report generation
        }
    }
}
