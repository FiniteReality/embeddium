package org.embeddedt.embeddium.bootstrap;

import cpw.mods.modlauncher.Launcher;
import cpw.mods.modlauncher.api.IModuleLayerManager;
import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.minecraftforge.fml.StartupMessageManager;
import net.minecraftforge.fml.loading.FMLLoader;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import net.minecraftforge.fml.loading.ImmediateWindowProvider;
import net.minecraftforge.fml.loading.LoadingModList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ServiceLoader;
import java.util.function.IntSupplier;

/**
 * This class is responsible for reimplementing the bootstrap of the FML progress window, but from a mod context.
 * We need to set up the window late to be able to apply various workarounds and have full control of the GL context's
 * setup flags. Otherwise, performance degradation or unexpected behavior may occur.
 */
public class EmbeddiumEarlyWindowHacks {
    private static final Logger LOGGER = LoggerFactory.getLogger("Embeddium-FMLEarlyWindow");

    public static void createEarlyLaunchWindow(IntSupplier width, IntSupplier height) {
        // Load the FML early window implementation
        var newProviderOpt = ServiceLoader.load(ImmediateWindowProvider.class)
                .stream()
                .map(ServiceLoader.Provider::get)
                .filter(p -> p.name().equals("fmlearlywindow"))
                .findFirst();

        if(newProviderOpt.isEmpty()) {
            LOGGER.warn("Failed to find FML early window implementation, aborting");
            return;
        }

        var newProvider = newProviderOpt.get();

        // Change the current provider used by FML to the new one
        try {
            Field f = ImmediateWindowHandler.class.getDeclaredField("provider");
            f.setAccessible(true);
            ImmediateWindowProvider oldProvider = (ImmediateWindowProvider)f.get(null);
            if(!oldProvider.name().equals("dummyprovider")) {
                LOGGER.error("Did not find dummy provider as we expected, found {}. Aborting.", oldProvider.getClass().getName());
                return;
            }
            f.set(null, newProviderOpt.get());
        } catch(ReflectiveOperationException e) {
            throw new RuntimeException("Exception setting new provider", e);
        }

        // Initialize the new provider
        FMLLoader.progressWindowTick = newProvider.initialize(new String[] {
                "--fml.mcVersion", FMLLoader.versionInfo().mcVersion(),
                "--fml.forgeVersion", FMLLoader.versionInfo().forgeVersion(),
                "--width", String.valueOf(width.getAsInt()),
                "--height", String.valueOf(height.getAsInt())
        });

        var modInfo = LoadingModList.get().getModFileById(SodiumClientMod.MODID);
        String ourVersion = modInfo != null ? modInfo.versionString() : "unknown";
        StartupMessageManager.modLoaderConsumer().ifPresent(c -> c.accept("Embeddium " + ourVersion));
        ImmediateWindowHandler.acceptGameLayer(Launcher.INSTANCE.findLayerManager().orElseThrow().getLayer(IModuleLayerManager.Layer.GAME).orElseThrow());

        // Workaround for Forge bug: make sure windowTick is actually populated before continuing
        try {
            Field windowTickField = newProvider.getClass().getDeclaredField("windowTick");
            windowTickField.setAccessible(true);
            while(windowTickField.get(newProvider) == null) {
                try {
                    Thread.sleep(100);
                } catch(InterruptedException ignored) {}
            }
        } catch(ReflectiveOperationException e) {
            LOGGER.error("Exception thrown while waiting for window tick to be present", e);
        }

        // The early window is now activated and has replaced the dummy provider.
        LOGGER.info("Successfully initialized our own early loading screen");
    }
}
