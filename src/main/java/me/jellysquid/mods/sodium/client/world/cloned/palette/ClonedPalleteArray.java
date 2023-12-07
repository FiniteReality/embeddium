package me.jellysquid.mods.sodium.client.world.cloned.palette;

public class ClonedPalleteArray<K> implements ClonedPalette<K> {
    private final K[] array;

    public ClonedPalleteArray(K[] array) {
        this.array = array;
    }

    @Override
    public K get(int id) {
        K value = this.array[id];
        // TODO: Remove this check?
        if (value == null) {
            throw new NullPointerException("Value for " + id + " is null");
        }
        return value;
    }
}
