package me.jellysquid.mods.sodium.mixin.workarounds.context_creation;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaWorkarounds;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import net.minecraftforge.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;


@Mixin(Window.class)
public class MixinWindow {

    @Unique
    private static final Logger sodium$LOGGER = LoggerFactory.getLogger("Sodium-EarlySetup");

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraftforge/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private long wrapGlfwCreateWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
>>>>>>> upstream/dev
        if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS)) {
            NvidiaWorkarounds.install();
        }

        /**
         * @author Asek3
         * Was taken from mixin.core due to impossibility of injecting into constructors on Forge
         */
        if (SodiumClientMod.options().performance.useNoErrorGLContext &&
                !Workarounds.isWorkaroundEnabled(Workarounds.Reference.NO_ERROR_CONTEXT_UNSUPPORTED)) {
            GLFW.glfwWindowHint(GLFW.GLFW_CONTEXT_NO_ERROR, GLFW.GLFW_TRUE);
        }

        try {
            return ImmediateWindowHandler.setupMinecraftWindow(width, height, title, monitor);
        } finally {
            if (Workarounds.isWorkaroundEnabled(Workarounds.Reference.NVIDIA_THREADED_OPTIMIZATIONS)) {
                NvidiaWorkarounds.uninstall();
            }
        }
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    private void postWindowCreated(WindowEventHandler eventHandler, MonitorTracker monitorTracker, WindowSettings settings, String videoMode, String title, CallbackInfo ci) {
        sodium$LOGGER.info("OpenGL Vendor: {}", GL11C.glGetString(GL11C.GL_VENDOR));
        sodium$LOGGER.info("OpenGL Renderer: {}", GL11C.glGetString(GL11C.GL_RENDERER));
        sodium$LOGGER.info("OpenGL Version: {}", GL11C.glGetString(GL11C.GL_VERSION));
    }
}
