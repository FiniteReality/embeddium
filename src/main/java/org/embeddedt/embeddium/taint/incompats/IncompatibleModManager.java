package org.embeddedt.embeddium.taint.incompats;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.neoforged.fml.ModLoader;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.ModLoadingStage;
import net.neoforged.fml.ModLoadingWarning;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforgespi.language.IModInfo;

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
        // TODO: Enable
        if (true || SodiumClientMod.options().advanced.disableIncompatibleModWarnings) {
            return;
        }
        IModInfo selfInfo = ModLoadingContext.get().getActiveContainer().getModInfo();
        String[] modDeclarationList = INCOMPATIBLE_MODS.stream().filter(ModDeclaration::matches).map(ModDeclaration::toString).toArray(String[]::new);
        if(modDeclarationList.length > 0) {
            event.enqueueWork(() -> {
                ModLoader.get().addWarning(new ModLoadingWarning(selfInfo, ModLoadingStage.SIDED_SETUP, "embeddium.conflicting_mod", String.join(", ", modDeclarationList)));
                ModLoader.get().addWarning(new ModLoadingWarning(selfInfo, ModLoadingStage.SIDED_SETUP, "embeddium.conflicting_mod_list", String.join(", ", modDeclarationList)));
            });
        }
    }
}
