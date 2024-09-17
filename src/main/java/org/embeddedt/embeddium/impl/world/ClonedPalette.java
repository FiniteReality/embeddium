package org.embeddedt.embeddium.impl.world;

import net.minecraft.nbt.ListTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.level.chunk.Palette;
import org.jetbrains.annotations.Nullable;

import java.util.function.Predicate;

public class ClonedPalette<T> implements Palette<T> {
    private final Object[] values;
    private final int size;

    public ClonedPalette(Palette<T> toClone, int bits) {
        var values = new Object[1 << bits];
        int i;
        for (i = 0; i < values.length; i++) {
            values[i] = toClone.valueFor(i);
            if (values[i] == null) {
                break;
            }
        }
        this.size = i;
        this.values = values;
    }

    @Override
    public int idFor(T state) {
        var values = this.values;
        var size = this.size;
        for (int i = 0; i < size; i++) {
            var v = values[i];
            if (v == state) {
                return i;
            } else if (v == null) {
                break;
            }
        }
        throw new IllegalStateException("Can't find value for: " + state);
    }

    @Override
    public boolean maybeHas(Predicate<T> filter) {
        return true;
    }

    @Override
    @SuppressWarnings("unchecked")
    public @Nullable T valueFor(int indexKey) {
        return indexKey >= 0 && indexKey < this.size ? (T)this.values[indexKey] : null;
    }

    @Override
    public void read(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int getSerializedSize() {
        return 0;
    }

    @Override
    public void read(ListTag nbt) {
        throw new UnsupportedOperationException();
    }
}
