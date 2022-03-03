package me.jellysquid.mods.sodium.client.gui.misc;

import java.util.Arrays;
import java.util.Comparator;
import net.minecraft.util.math.MathHelper;

public enum GraphicsMode {
    FAST(0, "options.graphics.fast"),
    FANCY(1, "options.graphics.fancy");

    private static final GraphicsMode[] VALUES;
    private final int id;
    private final String translationKey;

    private GraphicsMode(int j, String string2) {
        this.id = j;
        this.translationKey = string2;
    }

    public int getId() {
        return this.id;
    }

    public String getTranslationKey() {
        return this.translationKey;
    }

    public String toString() {
        switch (this) {
            case FAST: {
                return "fast";
            }
            case FANCY: {
                return "fancy";
            }
        }
        throw new IllegalArgumentException();
    }

    public static GraphicsMode byId(int id) {
        return VALUES[MathHelper.floorMod(id, VALUES.length)];
    }

    static {
        VALUES = (GraphicsMode[])Arrays.stream(GraphicsMode.values()).sorted(Comparator.comparingInt(GraphicsMode::getId)).toArray(GraphicsMode[]::new);
    }
}
