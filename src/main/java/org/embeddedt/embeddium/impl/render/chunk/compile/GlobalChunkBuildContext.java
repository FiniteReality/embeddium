package org.embeddedt.embeddium.impl.render.chunk.compile;

import me.jellysquid.mods.sodium.client.render.chunk.compile.ChunkBuildContext;
import me.jellysquid.mods.sodium.mixin.core.render.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import org.jetbrains.annotations.Nullable;

public final class GlobalChunkBuildContext {
    private static ChunkBuildContext mainThreadContext;
    private static final Thread mainThread = ((MinecraftAccessor) Minecraft.getInstance()).embeddium$getGameThread();

    private GlobalChunkBuildContext() {}

    public static void setMainThread() {
        if(mainThread != Thread.currentThread()) {
            throw new IllegalStateException("Global chunk build context captured wrong thread");
        }
    }

    @Nullable
    public static ChunkBuildContext get() {
        var thread = Thread.currentThread();
        // Main thread first, because it's the most common case
        if(thread == mainThread) {
            return mainThreadContext;
        } else if(thread instanceof Holder holder) {
            return holder.embeddium$getGlobalContext();
        } else {
            return null;
        }
    }

    public static void bindMainThread(ChunkBuildContext context) {
        mainThreadContext = context;
    }

    public interface Holder {
        ChunkBuildContext embeddium$getGlobalContext();
    }
}
