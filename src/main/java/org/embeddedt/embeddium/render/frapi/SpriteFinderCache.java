package org.embeddedt.embeddium.render.frapi;

import me.jellysquid.mods.sodium.client.SodiumClientMod;
import net.fabricmc.fabric.api.renderer.v1.model.SpriteFinder;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.inventory.InventoryMenu;
import org.jetbrains.annotations.Nullable;

import java.lang.invoke.*;

/**
 * Provides a mechanism for retrieving sprites by location on the atlas.
 */
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

    public static void onReload() {
        if(SPRITE_FINDER_HANDLE != null) {
            var listener = new SimpleSynchronousResourceReloadListener() {
                @Override
                public ResourceLocation getFabricId() {
                    return new ResourceLocation(SodiumClientMod.MODID, "sprite_finder_cache");
                }

                @Override
                public void onResourceManagerReload(ResourceManager pResourceManager) {
                    var modelManager = Minecraft.getInstance().getModelManager();
                    Finder finder = NULL_FINDER;
                    var fabricFinder = SpriteFinder.get(modelManager.getAtlas(InventoryMenu.BLOCK_ATLAS));
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
            ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(listener);
        }
    }

    public static Finder forBlockAtlas() {
        return blockAtlasSpriteFinder;
    }
}
