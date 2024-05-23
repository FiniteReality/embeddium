package org.embeddedt.embeddium.model.color.interop;

import net.minecraft.client.color.item.ItemColor;
import net.minecraft.world.item.ItemStack;

public interface ItemColorsExtended {
    ItemColor sodium$getColorProvider(ItemStack stack);
}
