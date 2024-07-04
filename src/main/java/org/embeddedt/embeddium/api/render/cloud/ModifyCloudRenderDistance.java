package org.embeddedt.embeddium.api.render.cloud;

import net.minecraft.client.Minecraft;

public class ModifyCloudRenderDistance {
    private static final ModifyCloudRenderDistance instance = new ModifyCloudRenderDistance();
    public static ModifyCloudRenderDistance instance() {
        return instance;
    }
    private int cloudDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
    private ModifyCloudRenderDistance() {}

    public int getCloudDistance() {
        return this.cloudDistance;
    }

    public void setCloudDistance(int distance) {
        if (distance < 1) {
            cloudDistance = Minecraft.getInstance().options.getEffectiveRenderDistance();
            return;
        }
        this.cloudDistance = distance;
    }
}
