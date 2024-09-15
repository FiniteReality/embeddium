package me.jellysquid.mods.sodium.client.compatibility.checks;

import me.jellysquid.mods.sodium.client.compatibility.workarounds.nvidia.NvidiaDriverVersion;
import me.jellysquid.mods.sodium.client.gui.console.Console;
import me.jellysquid.mods.sodium.client.gui.console.message.MessageLevel;
import net.minecraft.Util;
import net.minecraft.network.chat.Component;
import me.jellysquid.mods.sodium.client.compatibility.environment.GLContextInfo;
import net.minecraft.network.chat.TranslatableComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.lwjgl.opengl.ARBDebugOutput;
import org.lwjgl.opengl.GL11;

import static me.jellysquid.mods.sodium.client.SodiumClientMod.MODNAME;

/**
 * Performs OpenGL driver validation after the game creates an OpenGL context. This runs immediately after OpenGL
 * context creation, and uses the implementation details of the OpenGL context to perform validation.
 */
public class LateDriverScanner {
    private static final Logger LOGGER = LogManager.getLogger(MODNAME + "-PostlaunchChecks");

    public static void onContextInitialized() {
        checkContextImplementation();

        if (isUsingPojavLauncher()) {
            Console.instance().logMessage(MessageLevel.SEVERE, new TranslatableComponent("sodium.console.pojav_launcher"), 30.0);
            LOGGER.error("It appears that PojavLauncher is being used with an OpenGL compatibility layer. This will " +
                    "likely cause severe performance issues, graphical issues, and crashes when used with " + MODNAME + ". This " +
                    "configuration is not supported -- you are on your own!");
        }
    }

    private static void checkContextImplementation() {
        GLContextInfo driver = GLContextInfo.create();

        if (driver == null) {
            LOGGER.warn("Could not retrieve identifying strings for OpenGL implementation");
            return;
        }

        LOGGER.info("OpenGL Vendor: {}", driver.vendor());
        LOGGER.info("OpenGL Renderer: {}", driver.renderer());
        LOGGER.info("OpenGL Version: {}", driver.version());

        if (!isSupportedNvidiaDriver(driver)) {
            Console.instance()
                    .logMessage(MessageLevel.SEVERE, new TranslatableComponent("sodium.console.broken_nvidia_driver"), 30.0);

            LOGGER.error("The NVIDIA graphics driver appears to be out of date. This will likely cause severe " +
                    "performance issues and crashes when used with Sodium. The graphics driver should be updated to " +
                    "the latest version (version 536.23 or newer).");
        }

        if (driver.vendor() != null && driver.vendor().contains("NVIDIA")) {
            LOGGER.warn("Enabling secondary workaround for NVIDIA threaded optimizations");
            // https://github.com/godotengine/godot/issues/33969#issuecomment-917846774
            // Attempt to force the driver to disable its submission thread by enabling the synchronous debug output
            // flag. In Linux testing, this successfully suppresses bad behavior from the driver on both 470 and
            // 550 versions, even if __GL_THREADED_OPTIMIZATIONS=1 is explicitly provided.
            GL11.glEnable(ARBDebugOutput.GL_DEBUG_OUTPUT_SYNCHRONOUS_ARB);
        }
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/1486
    // The way which NVIDIA tries to detect the Minecraft process could not be circumvented until fairly recently
    // So we require that an up-to-date graphics driver is installed so that our workarounds can disable the Threaded
    // Optimizations driver hack.
    private static boolean isSupportedNvidiaDriver(GLContextInfo driver) {
        // The Linux driver has two separate branches which have overlapping version numbers, despite also having
        // different feature sets. As a result, we can't reliably determine which Linux drivers are broken...
        if (Util.getPlatform() != Util.OS.WINDOWS) {
            return true;
        }

        var version = NvidiaDriverVersion.tryParse(driver);

        if (version != null) {
            return !version.isWithinRange(
                    new NvidiaDriverVersion(526, 47), // Broken in 526.47
                    new NvidiaDriverVersion(536, 23) // Fixed in 536.23
            );
        }

        // If we couldn't determine the version, then it's supported either way.
        return true;
    }

    // https://github.com/CaffeineMC/sodium-fabric/issues/1916
    private static boolean isUsingPojavLauncher() {
        if (System.getenv("POJAV_RENDERER") != null) {
            LOGGER.warn("Detected presence of environment variable POJAV_LAUNCHER, which seems to indicate we are running on Android");

            return true;
        }

        var librarySearchPaths = System.getProperty("java.library.path", null);

        if (librarySearchPaths != null) {
            for (var path : librarySearchPaths.split(":")) {
                if (isKnownAndroidPathFragment(path)) {
                    LOGGER.warn("Found a library search path which seems to be hosted in an Android filesystem: {}", path);

                    return true;
                }
            }
        }

        var workingDirectory = System.getProperty("user.home", null);

        if (workingDirectory != null) {
            if (isKnownAndroidPathFragment(workingDirectory)) {
                LOGGER.warn("Working directory seems to be hosted in an Android filesystem: {}", workingDirectory);
            }
        }

        return false;
    }

    private static boolean isKnownAndroidPathFragment(String path) {
        return path.matches("/data/user/[0-9]+/net\\.kdt\\.pojavlaunch");
    }
}
