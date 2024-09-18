package me.jellysquid.mods.sodium.client.data.fingerprint;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import me.jellysquid.mods.sodium.client.util.FileUtil;
import net.fabricmc.loader.api.FabricLoader;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class HashedFingerprint {
    public static final int CURRENT_VERSION = 1;
    @SerializedName("v")
    private final Integer version;
    @SerializedName("s")
    private final @NotNull String saltHex;
    @SerializedName("u")
    private final @NotNull String uuidHashHex;
    @SerializedName("p")
    private final @NotNull String pathHashHex;
    @SerializedName("t")
    private final long timestamp;

    public HashedFingerprint(
            Integer version,

            @NotNull
            String saltHex,

            @NotNull
            String uuidHashHex,

            @NotNull
            String pathHashHex,

            long timestamp) {
        this.version = version;
        this.saltHex = saltHex;
        this.uuidHashHex = uuidHashHex;
        this.pathHashHex = pathHashHex;
        this.timestamp = timestamp;
    }

    public static @Nullable HashedFingerprint loadFromDisk() {
        Path path = getFilePath();

        if (!Files.exists(path)) {
            return null;
        }

        HashedFingerprint data;

        try {
            data = new Gson()
                    .fromJson(Files.readString(path), HashedFingerprint.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to load data file", e);
        }

        if (data.version() != CURRENT_VERSION) {
            return null;
        }

        return data;
    }

    public static void writeToDisk(@NotNull HashedFingerprint data) {
        Objects.requireNonNull(data);

        try {
            FileUtil.writeTextRobustly(new Gson()
                    .toJson(data), getFilePath());
        } catch (IOException e) {
            throw new RuntimeException("Failed to save data file", e);
        }
    }

    private static Path getFilePath() {
        return FabricLoader.getInstance().getConfigDir()
                .resolve("embeddium-fingerprint.json");
    }

    public Integer version() {
        return version;
    }

    public @NotNull String saltHex() {
        return saltHex;
    }

    public @NotNull String uuidHashHex() {
        return uuidHashHex;
    }

    public @NotNull String pathHashHex() {
        return pathHashHex;
    }

    public long timestamp() {
        return timestamp;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (HashedFingerprint) obj;
        return Objects.equals(this.version, that.version) &&
                Objects.equals(this.saltHex, that.saltHex) &&
                Objects.equals(this.uuidHashHex, that.uuidHashHex) &&
                Objects.equals(this.pathHashHex, that.pathHashHex) &&
                this.timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, saltHex, uuidHashHex, pathHashHex, timestamp);
    }

    @Override
    public String toString() {
        return "HashedFingerprint[" +
                "version=" + version + ", " +
                "saltHex=" + saltHex + ", " +
                "uuidHashHex=" + uuidHashHex + ", " +
                "pathHashHex=" + pathHashHex + ", " +
                "timestamp=" + timestamp + ']';
    }

}
