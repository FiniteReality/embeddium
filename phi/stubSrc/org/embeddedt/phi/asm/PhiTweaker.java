package org.embeddedt.phi.asm;

import com.gtnewhorizons.retrofuturabootstrap.Main;
import com.gtnewhorizons.retrofuturabootstrap.api.RetroFuturaBootstrap;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;
import com.gtnewhorizons.rfbplugins.compat.ModernJavaCompatibilityPlugin;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
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

        // Inject MOD_CLASSES
        String modClasses = System.getenv("MOD_CLASSES");
        if(modClasses != null && !modClasses.isEmpty()) {
            for(String listEntry : modClasses.split(":")) {
                String[] entry = listEntry.split("%%");
                try {
                    classLoader.addURL(new File(entry[1]).toURI().toURL());
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        Mixins.addConfiguration("embeddium.mixins.json");
        return new String[] { "--accessToken", "0", "--version", "0.0.0" };
    }
}
