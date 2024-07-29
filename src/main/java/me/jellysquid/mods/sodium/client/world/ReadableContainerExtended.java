package me.jellysquid.mods.sodium.client.world;

import net.minecraft.world.level.chunk.PalettedContainer;

public interface ReadableContainerExtended<T> {
    @SuppressWarnings("unchecked")
    static <T> ReadableContainerExtended<T> of(PalettedContainer<T> container) {
        return (ReadableContainerExtended<T>) container;
    }

    static <T> PalettedContainer<T> clone(PalettedContainer<T> container) {
        if (container == null) {
            return null;
        }

        return of(container).sodium$copy();
    }

    void sodium$unpack(T[] values);
    void sodium$unpack(T[] values, int minX, int minY, int minZ, int maxX, int maxY, int maxZ);

    PalettedContainer<T> sodium$copy();
}
