package me.jellysquid.mods.sodium.client.gui.options.binding.compat;

import me.jellysquid.mods.sodium.client.gui.options.binding.OptionBinding;
import net.minecraft.client.BooleanOption;
import net.minecraft.client.Options;

public class VanillaBooleanOptionBinding implements OptionBinding<Options, Boolean> {
    private final BooleanOption option;

    public VanillaBooleanOptionBinding(BooleanOption option) {
        this.option = option;
    }

    @Override
    public void setValue(Options storage, Boolean value) {
        this.option.set(storage, value.toString());
    }

    @Override
    public Boolean getValue(Options storage) {
        return this.option.get(storage);
    }
}
