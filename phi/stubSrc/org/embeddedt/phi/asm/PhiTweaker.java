package org.embeddedt.phi.asm;

import com.gtnewhorizons.retrofuturabootstrap.Main;
import com.gtnewhorizons.retrofuturabootstrap.api.RetroFuturaBootstrap;
import com.gtnewhorizons.retrofuturabootstrap.api.RfbClassTransformer;
import com.gtnewhorizons.rfbplugins.compat.ModernJavaCompatibilityPlugin;
import com.llamalad7.mixinextras.MixinExtrasBootstrap;
import net.minecraft.launchwrapper.ITweaker;
import net.minecraft.launchwrapper.LaunchClassLoader;
import org.spongepowered.asm.mixin.Mixins;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PhiTweaker implements ITweaker {
    private static final boolean PRODUCTION = Boolean.getBoolean("phi.production");

    private List<String> extraArgs = List.of();
    @Override
    public void acceptOptions(List<String> args, File gameDir, File assetsDir, String profile) {
        extraArgs = new ArrayList<>(args);
        if(gameDir != null) {
            extraArgs.add("--gameDir");
            extraArgs.add(gameDir.getAbsolutePath());
        }
        if(assetsDir != null) {
            extraArgs.add("-assetsDir");
            extraArgs.add(assetsDir.getAbsolutePath());
        }
    }

    @Override
    public void injectIntoClassLoader(LaunchClassLoader classLoader) {
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

        if(PRODUCTION) {
            classLoader.registerTransformer("org.embeddedt.phi.asm.PhiPatchingTransformer");
        }
    }

    @Override
    public String getLaunchTarget() {
        return "net.minecraft.client.main.Main";
    }

    @Override
    public String[] getLaunchArguments() {
        if(RetroFuturaBootstrap.API.launchClassLoader().asURLClassLoader().findResource("embeddium.mixins.json") != null) {
            Mixins.addConfiguration("embeddium.mixins.json");
        }
        List<String> args = new ArrayList<>(extraArgs);
        args.add("--version");
        args.add(Main.initialGameVersion);
        MixinExtrasBootstrap.init();
        return args.toArray(new String[0]);
    }
}
