package net.neoforged.neoforge.client.event;

import net.minecraft.client.Minecraft;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.neoforged.bus.api.Event;

import java.util.ArrayList;
import java.util.List;

public class RegisterClientReloadListenersEvent extends Event {
    public void registerReloadListener(PreparableReloadListener listener) {
        ((ReloadableResourceManager)Minecraft.getInstance().getResourceManager()).registerReloadListener(listener);
    }
}
