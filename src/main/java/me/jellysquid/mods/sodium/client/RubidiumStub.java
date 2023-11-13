package me.jellysquid.mods.sodium.client;

import net.neoforged.fml.IExtensionPoint;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;

@Mod("rubidium")
public class RubidiumStub {
    public RubidiumStub() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "rubidium", (a, b) -> true));
    }
}
