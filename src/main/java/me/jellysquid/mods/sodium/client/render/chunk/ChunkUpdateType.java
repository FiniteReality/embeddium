package me.jellysquid.mods.sodium.client.render.chunk;

public enum ChunkUpdateType {
    SORT(false, Integer.MAX_VALUE),
    IMPORTANT_SORT(true, Integer.MAX_VALUE),
    INITIAL_BUILD(false, 128),
    REBUILD(false, Integer.MAX_VALUE),
    IMPORTANT_REBUILD(true, Integer.MAX_VALUE);

    private final boolean important;
    private final int maximumQueueSize;

    @Deprecated
    ChunkUpdateType(boolean important) {
        this(important, 32);
    }

    ChunkUpdateType(boolean important, int maximumQueueSize) {
        this.important = important;
        this.maximumQueueSize = maximumQueueSize;
    }

    public boolean isImportant() {
        return this.important;
    }

    public int getMaximumQueueSize() {
        return this.maximumQueueSize;
    }

    public static boolean isSort(ChunkUpdateType type) {
        return type == SORT || type == IMPORTANT_SORT;
    }
}
