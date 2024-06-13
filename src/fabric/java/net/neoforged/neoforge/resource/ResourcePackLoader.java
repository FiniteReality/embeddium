package net.neoforged.neoforge.resource;

import net.minecraft.server.packs.repository.Pack;

import java.util.Optional;

public class ResourcePackLoader {
    public static Optional<Pack.ResourcesSupplier> getPackFor(String modId) {
        return Optional.empty();
    }
}
