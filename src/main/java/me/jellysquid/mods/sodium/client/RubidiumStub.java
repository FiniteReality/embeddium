package me.jellysquid.mods.sodium.client;

import net.minecraftforge.fml.IExtensionPoint;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;

@Mod("rubidium")
public class RubidiumStub {
    public RubidiumStub() {
        ModLoadingContext.get().registerExtensionPoint(IExtensionPoint.DisplayTest.class, () -> new IExtensionPoint.DisplayTest(() -> "rubidium", (a, b) -> true));
    }
}
