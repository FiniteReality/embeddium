package org.embeddedt.embeddium.impl.mixin.core.model;

import net.minecraft.client.resources.model.SimpleBakedModel;
import org.embeddedt.embeddium.impl.model.quad.BakedQuadView;
import org.embeddedt.embeddium.impl.model.quad.properties.ModelQuadFlags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(SimpleBakedModel.Builder.class)
public class SimpleBakedModelBuilderMixin {
    @ModifyArg(method = { "addCulledFace", "addUnculledFace" }, at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z", remap = false), require = 0)
    private Object setVanillaShadingFlag(Object quad) {
        BakedQuadView view = (BakedQuadView)quad;
        view.setFlags(view.getFlags() | ModelQuadFlags.IS_VANILLA_SHADED);
        return quad;
    }
}
