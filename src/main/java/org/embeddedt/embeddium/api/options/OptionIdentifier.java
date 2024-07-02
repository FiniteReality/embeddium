package org.embeddedt.embeddium.api.options;

import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public final class OptionIdentifier<T> {
    private final String modId;
    private final String path;
    private final Class<T> clz;

    private static final ObjectOpenHashSet<OptionIdentifier<?>> IDENTIFIERS = new ObjectOpenHashSet<>();

    /**
     * Constant {@link OptionIdentifier} that may be used to avoid returning null values.
     */
    public static final OptionIdentifier<Void> EMPTY = create("", "", Void.class);

    private OptionIdentifier(String modId, String path, Class<T> clz) {
        this.modId = modId;
        this.path = path;
        this.clz = clz;
    }

    public String getModId() {
        return this.modId;
    }

    public String getPath() {
        return this.path;
    }

    public Class<T> getType() {
        return this.clz;
    }

    public static OptionIdentifier<Void> create(ResourceLocation location) {
        return create(location, void.class);
    }

    public static <T> OptionIdentifier<T> create(ResourceLocation location, Class<T> clz) {
        return create(location.getNamespace(), location.getPath(), clz);
    }

    public static OptionIdentifier<Void> create(String modId, String path) {
        return create(modId, path, void.class);
    }

    @SuppressWarnings("unchecked")
    public static synchronized <T> OptionIdentifier<T> create(String modId, String path, Class<T> clz) {
        OptionIdentifier<T> ourIdentifier = new OptionIdentifier<>(modId, path, clz);
        OptionIdentifier<T> oldIdentifier = (OptionIdentifier<T>)IDENTIFIERS.addOrGet(ourIdentifier);
        if(oldIdentifier != null && oldIdentifier.clz != ourIdentifier.clz) {
            throw new IllegalArgumentException(String.format("OptionIdentifier '%s' created with differing class type %s from existing instance %s", ourIdentifier, ourIdentifier.clz, oldIdentifier.clz));
        }
        return oldIdentifier;
    }

    public static boolean isPresent(@Nullable OptionIdentifier<?> id) {
        return id != null && id != EMPTY;
    }
    
    public boolean matches(OptionIdentifier<?> other) {
        return this == other;
    }
    
    public boolean matches(ResourceLocation other) {
        return this.modId.equals(other.getNamespace()) && 
               this.path.equals(other.getPath());
    }

    @Override
    public String toString() {
        return this.modId + ":" + this.path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OptionIdentifier<?> that = (OptionIdentifier<?>) o;
        return Objects.equals(modId, that.modId) && Objects.equals(path, that.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(modId, path);
    }
}
