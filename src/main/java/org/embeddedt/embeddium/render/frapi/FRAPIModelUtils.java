package org.embeddedt.embeddium.render.frapi;

import net.fabricmc.fabric.api.renderer.v1.model.FabricBakedModel;
import net.minecraft.client.resources.model.BakedModel;

public class FRAPIModelUtils {
    public static boolean isFRAPIModel(BakedModel model) {
        if(!FRAPIRenderHandler.INDIGO_PRESENT) {
            return false;
        }

        return !((FabricBakedModel)model).isVanillaAdapter();
    }
}
