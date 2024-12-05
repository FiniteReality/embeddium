package org.embeddedt.embeddium.impl.util.collections;

import net.minecraft.util.RandomSource;
import org.jetbrains.annotations.Nullable;

public interface WeightedRandomListExtended<E> {
    @Nullable
    E embeddium$getRandomItem(RandomSource random);
}
