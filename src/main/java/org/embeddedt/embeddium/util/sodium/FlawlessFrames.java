package org.embeddedt.embeddium.util.sodium;

import net.fabricmc.loader.api.FabricLoader;
import org.embeddedt.embeddium.api.service.FlawlessFramesService;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Implements the "Flawless Frames" FREX feature using which third-party mods can instruct Embeddium to sacrifice
 * performance (even beyond the point where it can no longer achieve interactive frame rates) in exchange for
 * a noticeable boost to quality.
 *
 * In Embeddium's case, this means waiting for all chunks to be fully updated and ready for rendering before each frame.
 *
 * The package name of this class should contain ".sodium." for proper detection by other mods.
 *
 * See https://github.com/grondag/frex/pull/9
 */
public class FlawlessFrames {
    private static final Set<Object> ACTIVE = ConcurrentHashMap.newKeySet();

    @SuppressWarnings("unchecked")
    public static void onClientInitialization() {
        Function<String, Consumer<Boolean>> provider = name -> {
            Object token = new Object();
            return active -> {
                if (active) {
                    ACTIVE.add(token);
                } else {
                    ACTIVE.remove(token);
                }
            };
        };
        // Fabric entrypoint
        try {
            FabricLoader.getInstance()
                    .getEntrypoints("frex_flawless_frames", Consumer.class)
                    .forEach(api -> api.accept(provider));
        } catch(NoClassDefFoundError ignored) {
        }
        // Platform-independent entrypoint
        ServiceLoader.load(FlawlessFramesService.class, FlawlessFrames.class.getClassLoader()).stream()
                .map(ServiceLoader.Provider::get)
                .forEach(s -> s.acceptController(provider));
    }

    public static boolean isActive() {
        return !ACTIVE.isEmpty();
    }
}
