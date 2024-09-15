package me.jellysquid.mods.sodium.mixin.core.model.colors;

import it.unimi.dsi.fastutil.objects.Reference2ReferenceMap;
import it.unimi.dsi.fastutil.objects.Reference2ReferenceOpenHashMap;
import me.jellysquid.mods.sodium.client.model.color.interop.ItemColorsExtended;
import net.minecraft.client.color.item.ItemColor;
import net.minecraft.client.color.item.ItemColors;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemColors.class)
public class ItemColorsMixin implements ItemColorsExtended {
    @Unique
    private final Reference2ReferenceMap<ItemLike, ItemColor> itemsToColor =
            new Reference2ReferenceOpenHashMap<>();

    @Inject(method = "register", at = @At("TAIL"))
    private void preRegisterColor(ItemColor provider, ItemLike[] items, CallbackInfo ci) {
        // Synchronize so the inevitable crash mods cause will come from the vanilla map
        synchronized (this.itemsToColor) {
            for (ItemLike convertible : items) {
                this.itemsToColor.put(convertible.asItem(), provider);
            }
        }
    }

    @Override
    public ItemColor sodium$getColorProvider(ItemStack stack) {
        return this.itemsToColor.get(stack.getItem());
    }
}
