package me.jellysquid.mods.sodium.mixin.workarounds.context_creation;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import me.jellysquid.mods.sodium.client.util.workarounds.PostLaunchChecks;
import me.jellysquid.mods.sodium.client.util.workarounds.Workarounds;
import me.jellysquid.mods.sodium.client.util.workarounds.driver.nvidia.NvidiaWorkarounds;
import me.jellysquid.mods.sodium.client.util.workarounds.platform.windows.WindowsModuleChecks;
import net.minecraft.client.WindowEventHandler;
import net.minecraft.client.WindowSettings;
import net.minecraft.client.util.MonitorTracker;
import net.minecraft.client.util.Window;
import net.minecraft.util.Util;
import net.neoforged.fml.loading.ImmediateWindowHandler;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.WGL;
import org.lwjgl.system.MemoryUtil;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.IntSupplier;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

import static me.jellysquid.mods.sodium.client.SodiumClientMod.MODNAME;


@Mixin(Window.class)
public class WindowMixin {
    @Shadow
    @Final
    private static Logger LOGGER;

    @Unique
    private long wglPrevContext = MemoryUtil.NULL;

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/neoforged/fml/loading/ImmediateWindowHandler;setupMinecraftWindow(Ljava/util/function/IntSupplier;Ljava/util/function/IntSupplier;Ljava/util/function/Supplier;Ljava/util/function/LongSupplier;)J"))
    private long wrapGlfwCreateWindow(IntSupplier width, IntSupplier height, Supplier<String> title, LongSupplier monitor) {
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
        // Capture the current WGL context so that we can detect it being replaced later.
        if (Util.getOperatingSystem() == Util.OperatingSystem.WINDOWS) {
            this.wglPrevContext = WGL.wglGetCurrentContext();
        } else {
            this.wglPrevContext = MemoryUtil.NULL;
        }

        PostLaunchChecks.checkContext();
    }

    @Inject(method = "swapBuffers", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;flipFrame(J)V", shift = At.Shift.AFTER))
    private void preSwapBuffers(CallbackInfo ci) {
        if (this.wglPrevContext == MemoryUtil.NULL) {
            // There is no prior recorded context.
            return;
        }

        var context = WGL.wglGetCurrentContext();

        if (this.wglPrevContext == context) {
            // The context has not changed.
            return;
        }

        // Something has decided to replace the OpenGL context, which is not a good sign
        LOGGER.warn("The OpenGL context appears to have been suddenly replaced! Something has likely just injected into the game process.");

        // Likely, this indicates a module was injected into the current process. We should check that
        // nothing problematic was just installed.
        WindowsModuleChecks.checkModules();

        // If we didn't find anything problematic (which would have thrown an exception), then let's just record
        // the new context pointer and carry on.
        this.wglPrevContext = context;
    }
}
