package org.embeddedt.embeddium.impl.mixin.core.collections;

import com.google.common.collect.ImmutableList;
import net.minecraft.util.RandomSource;
import net.minecraft.util.random.WeightedEntry;
import net.minecraft.util.random.WeightedRandomList;
import org.embeddedt.embeddium.impl.util.collections.WeightedRandomListExtended;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

import java.util.List;

@Mixin(WeightedRandomList.class)
public class WeightedRandomListMixin<E extends WeightedEntry> implements WeightedRandomListExtended<E> {
    @Shadow
    @Final
    private ImmutableList<E> items;

    @Shadow
    @Final
    private int totalWeight;

    @Override
    public @Nullable E embeddium$getRandomItem(RandomSource random) {
        return getAt(this.items, random.nextInt(this.totalWeight));
    }

    @Unique
    private static <T extends WeightedEntry> T getAt(List<T> pool, int totalWeight) {
        int i = 0;
        int len = pool.size();

        T weighted;

        do {
            if (i >= len) {
                return null;
            }

            weighted = pool.get(i++);
            totalWeight -= weighted.getWeight().asInt();
        } while (totalWeight >= 0);

        return weighted;
    }
}
