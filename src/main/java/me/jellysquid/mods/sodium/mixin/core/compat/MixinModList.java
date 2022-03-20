package me.jellysquid.mods.sodium.mixin.core.compat;

import java.util.Map;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModList;

//fck coremodding on forge. For BetterEnd compatibility
@Mixin(ModList.class)
public class MixinModList {
	
	@Shadow
	private Map<String, ModContainer> indexedMods;
	
	@Overwrite(remap = false)
    public boolean isLoaded(String modTarget)
    {
        return modTarget.equals("magnesium") ? true : this.indexedMods.containsKey(modTarget);
    }
	
}
