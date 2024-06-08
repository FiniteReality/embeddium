package org.embeddedt.embeddium.taint.incompats;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraftforge.fml.*;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.forgespi.language.IModInfo;

import java.util.List;

/**
 * Detect mods that are known to cause bugs (particularly with Embeddium present) and complain to the user.
 */
public class IncompatibleModManager {
    private static final List<ModDeclaration> INCOMPATIBLE_MODS = ImmutableList.<ModDeclaration>builder()
            .add(new ModDeclaration.Single("entityculling", "Entity Culling"))
            .add(new ModDeclaration.Single("sound_physics_remastered", "Sound Physics Remastered"))
            .build();

    public static void checkMods(FMLClientSetupEvent event) {
        IModInfo selfInfo = ModLoadingContext.get().getActiveContainer().getModInfo();
        if(ModList.get().isLoaded("lazurite")) {
            event.enqueueWork(() -> {
                ModLoader.get().addWarning(new ModLoadingWarning(selfInfo, ModLoadingStage.SIDED_SETUP, "Embeddium includes FFAPI support as of 0.3.20, Lazurite should be removed"));
            });
        }
        // TODO: Enable
        if (true || SodiumClientMod.options().advanced.disableIncompatibleModWarnings) {
            return;
        }
        String[] modDeclarationList = INCOMPATIBLE_MODS.stream().filter(ModDeclaration::matches).map(ModDeclaration::toString).toArray(String[]::new);
        if(modDeclarationList.length > 0) {
            event.enqueueWork(() -> {
                ModLoader.get().addWarning(new ModLoadingWarning(selfInfo, ModLoadingStage.SIDED_SETUP, "embeddium.conflicting_mod", String.join(", ", modDeclarationList)));
                ModLoader.get().addWarning(new ModLoadingWarning(selfInfo, ModLoadingStage.SIDED_SETUP, "embeddium.conflicting_mod_list", String.join(", ", modDeclarationList)));
            });
        }
    }
}
