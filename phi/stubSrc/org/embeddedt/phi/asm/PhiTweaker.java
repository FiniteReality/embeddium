package org.embeddedt.phi.asm;

import com.gtnewhorizons.retrofuturabootstrap.Main;
import com.gtnewhorizons.retrofuturabootstrap.api.RetroFuturaBootstrap;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;
import com.gtnewhorizons.rfbplugins.compat.ModernJavaCompatibilityPlugin;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhiTweaker implements ITweaker {
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {

    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
        // Fix classloading of SLF4J
        if(RetroFuturaBootstrap.API.launchClassLoader() instanceof LaunchClassLoader lcl) {
            // Make sure SLF4J is loaded by app classloader
            lcl.addClassLoaderExclusion("org.slf4j.");
        }
        RetroFuturaBootstrap.API.compatClassLoader().addClassLoaderExclusion("org.slf4j.");

        // Yeet modern Java transformer, this is modern Minecraft
        ModernJavaCompatibilityPlugin plugin = new ModernJavaCompatibilityPlugin();
        var idsToRemove = Arrays.stream(plugin.makeTransformers()).map(t -> "rfb-modern-java:" + t.id()).collect(Collectors.toSet());
        Main.mutateRfbTransformers(list -> {
            list.removeIf(transformer -> idsToRemove.contains(transformer.id()));
        });
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        return new String[] { "--accessToken", "0", "--version", "0.0.0" };
    }
}
