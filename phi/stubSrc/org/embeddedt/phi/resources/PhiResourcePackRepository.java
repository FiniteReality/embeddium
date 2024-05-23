package org.embeddedt.phi.resources;

import net.minecraft.network.chat.Component;
import net.minecraft.server.packs.*;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraft.server.packs.repository.RepositorySource;
import net.neoforged.fml.ModList;

import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public class PhiResourcePackRepository implements RepositorySource {
    static boolean embeddiumExists = false;
    static {
        // Load our mod class here
        try {
            Class<?> clz = Class.forName("me.jellysquid.mods.sodium.client.SodiumClientMod");
            embeddiumExists = true;
            clz.getConstructor().newInstance();
        } catch(Throwable e) {
            e.printStackTrace();
        }
    }

    @Override
    public void loadPacks(Consumer<Pack> consumer) {
        if(!embeddiumExists) {
            return;
        }
        Path resourcePackPath = ModList.get().getModFiles().get(0).getFile().findResource("pack.mcmeta").getParent();
        var packLocation = new PackLocationInfo("embeddium", Component.literal("Embeddium resources"), PackSource.BUILT_IN, Optional.empty());
        var packSelectionConfig = new PackSelectionConfig(true, Pack.Position.TOP, false);
        consumer.accept(Pack.readMetaAndCreate(packLocation, new PathPackResources.PathResourcesSupplier(resourcePackPath), PackType.CLIENT_RESOURCES, packSelectionConfig));
    }
}
