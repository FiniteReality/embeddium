package me.jellysquid.mods.sodium.client.world.biome;

public final class QuartPos {
    private QuartPos() {
    }

    public static int fromBlock(int blockCoord) {
        return blockCoord >> 2;
    }

    public static int quartLocal(int i) {
        return i & 3;
    }

    public static int toBlock(int biomeCoord) {
        return biomeCoord << 2;
    }

    public static int fromSection(int chunkCoord) {
        return chunkCoord << 2;
    }

    public static int toSection(int biomeCoord) {
        return biomeCoord >> 2;
    }
}
