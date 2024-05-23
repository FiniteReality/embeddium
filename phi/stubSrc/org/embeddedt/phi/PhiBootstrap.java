package org.embeddedt.phi;

import com.gtnewhorizons.retrofuturabootstrap.Main;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PhiBootstrap {
    public static void main(String[] args) throws Throwable {
        List<String> argsList = new ArrayList<>();
        Collections.addAll(argsList, args);
        argsList.add("--tweakClass");
        argsList.add("org.embeddedt.phi.asm.PhiTweaker");
        argsList.add("--tweakClass");
        argsList.add("org.spongepowered.asm.launch.MixinTweaker");
        var mixinConfigStream = PhiBootstrap.class.getClassLoader().getResourceAsStream("embeddium.mixins.json");
        if(mixinConfigStream != null) {
            mixinConfigStream.close();
            argsList.add("--mixin.config");
            argsList.add("embeddium.mixins.json");
        } else {
            System.err.println("Embeddium mixin config not found, launching without");
        }
        System.setProperty("embeddium.phi", "true");
        Main.main(argsList.toArray(new String[0]));
    }
}
