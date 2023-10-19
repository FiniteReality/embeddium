package me.jellysquid.mods.sodium.client.render.chunk;

public enum ChunkUpdateType {
    SORT(false),
    IMPORTANT_SORT(true),
    INITIAL_BUILD(false),
    REBUILD(false),
    IMPORTANT_REBUILD(true);

    private final boolean important;

    ChunkUpdateType(boolean important) {
        this.important = important;
    }

    public boolean isImportant() {
        return this.important;
    }

    public static boolean isSort(ChunkUpdateType type) {
        return type == SORT || type == IMPORTANT_SORT;
    }
}
