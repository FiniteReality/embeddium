package org.embeddedt.embeddium.impl.render.frapi;

import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.TextureContents;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;
import org.embeddedt.embeddium.impl.Embeddium;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;

/**
 * Provides a mechanism for retrieving sprites by location on the atlas.
 */
@EventBusSubscriber(modid = Embeddium.MODID, value = Dist.CLIENT, bus = EventBusSubscriber.Bus.MOD)
public class SpriteFinderCache {
    private static final Finder NULL_FINDER = (u, v) -> null;
    private static final MethodHandle SPRITE_FINDER_HANDLE;

    private static Finder blockAtlasSpriteFinder = NULL_FINDER;

    static {
        MethodHandle mh;
        try {
            mh = MethodHandles.lookup().findVirtual(Class.forName("net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder"), "find", MethodType.methodType(TextureAtlasSprite.class, float.class, float.class));
        } catch(Throwable e) {
            mh = null;
        }
        SPRITE_FINDER_HANDLE = mh;
    }

    public interface Finder {
        @Nullable
        TextureAtlasSprite findNearestSprite(float u, float v);
    }

    @SubscribeEvent
    public static void onReload(RegisterClientReloadListenersEvent event) {
        if(SPRITE_FINDER_HANDLE != null) {
            var listener = new SimplePreparableReloadListener<>() {
                @Override
                protected Object prepare(ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                    return null;
                }

                @Override
                protected void apply(Object pObject, ResourceManager pResourceManager, ProfilerFiller pProfiler) {
                    var modelManager = Minecraft.getInstance().getModelManager();
                    Finder finder = NULL_FINDER;
                    var fabricFinder = SpriteFinder.get(modelManager.getAtlas(TextureAtlas.LOCATION_BLOCKS));
                    try {
                        MethodType bootstrapType = MethodType.methodType(Finder.class, SpriteFinder.class);
                        MethodType invocationType = MethodType.methodType(TextureAtlasSprite.class, float.class, float.class);
                        // Adapt to our Finder interface
                        finder = (Finder)LambdaMetafactory.metafactory(MethodHandles.lookup(),
                                "findNearestSprite",
                                bootstrapType, invocationType,
                                SPRITE_FINDER_HANDLE, invocationType).getTarget().invokeExact((SpriteFinder)fabricFinder);
                    } catch(Throwable e) {
                        e.printStackTrace();
                    }
                    blockAtlasSpriteFinder = finder;
                }
            };
            event.registerReloadListener(listener);
        }
    }

    public static Finder forBlockAtlas() {
        return blockAtlasSpriteFinder;
    }
}
